package im.zhaojun.zfile.service.impl;

import cn.hutool.core.util.StrUtil;
import com.sun.istack.NotNull;
import im.zhaojun.zfile.exception.FileUploadFailException;
import im.zhaojun.zfile.exception.InitializeDriveException;
import im.zhaojun.zfile.exception.MakeDirFailException;
import im.zhaojun.zfile.exception.NotExistFileException;
import im.zhaojun.zfile.model.constant.StorageConfigConstant;
import im.zhaojun.zfile.model.constant.SystemConfigConstant;
import im.zhaojun.zfile.model.constant.ZFileConstant;
import im.zhaojun.zfile.model.dto.FileItemDTO;
import im.zhaojun.zfile.model.entity.StorageConfig;
import im.zhaojun.zfile.model.entity.SystemConfig;
import im.zhaojun.zfile.model.enums.FileTypeEnum;
import im.zhaojun.zfile.model.enums.StorageTypeEnum;
import im.zhaojun.zfile.repository.SystemConfigRepository;
import im.zhaojun.zfile.service.StorageConfigService;
import im.zhaojun.zfile.service.base.AbstractBaseFileService;
import im.zhaojun.zfile.service.base.BaseFileService;
import im.zhaojun.zfile.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import javax.annotation.Resource;
import javax.transaction.NotSupportedException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhaojun
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LocalServiceImpl extends AbstractBaseFileService implements BaseFileService {

    private static final Logger log = LoggerFactory.getLogger(LocalServiceImpl.class);

    @Resource
    private StorageConfigService storageConfigService;

    @Resource
    private SystemConfigRepository systemConfigRepository;

    /** 服务初始化完成标识，服务每次使用前需要初始化完成后才能正常 使用 */
    private ThreadLocal<Boolean> isInitialized = new ThreadLocal<>();
    /** 本地存储文件的根路径绝对地址，例如： D:/zfileData */
    private ThreadLocal<String> filePath = new ThreadLocal<>();

    public LocalServiceImpl() {
        isInitialized.set(false);
    }

    @Override
    public boolean isInitialized() {
        return isInitialized.get();
    }

    @Override
    public void init(Integer driveId) {
        if(Boolean.TRUE.equals(isInitialized.get())) {
            return;
        }
        this.setDriveId(driveId);
        Map<String, StorageConfig> stringStorageConfigMap =
                storageConfigService.selectStorageConfigMapByDriveId(driveId);
        this.mergeStrategyConfig(stringStorageConfigMap);
        filePath.set(stringStorageConfigMap.get(StorageConfigConstant.FILE_PATH_KEY).getValue());
        if (Objects.isNull(filePath.get())) {
            log.debug("初始化存储策略 [{}] 失败: 参数不完整", getStorageTypeEnum().getDescription());
            isInitialized.set(false);
            return;
        }

        File file = new File(filePath.get());
        if (!file.exists()) {
            throw new InitializeDriveException("文件路径: \"" + file.getAbsolutePath() + "\"不存在, 请检查是否填写正确.");
        } else {
            testConnection();
            isInitialized.set(true);
        }
    }


    @Override
    public List<FileItemDTO> fileList(String path) throws FileNotFoundException {
        if (StrUtil.startWith(path, "..") || StrUtil.startWith(path, "/..")) {
            return Collections.emptyList();
        }
        List<FileItemDTO> fileItemList = new ArrayList<>();

        String fullPath = StringUtils.removeDuplicateSeparator(filePath.get() + path);

        File file = new File(fullPath);

        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在");
        }

        File[] files = file.listFiles();

        if (files == null) {
            return fileItemList;
        }
        for (File f : files) {
            FileItemDTO fileItemDTO = new FileItemDTO();
            fileItemDTO.setType(f.isDirectory() ? FileTypeEnum.FOLDER : FileTypeEnum.FILE);
            fileItemDTO.setTime(new Date(f.lastModified()));
            fileItemDTO.setSize(f.length());
            fileItemDTO.setName(f.getName());
            fileItemDTO.setPath(path);
            if (f.isFile()) {
                fileItemDTO.setUrl(getDownloadUrl(StringUtils.concatUrl(path, f.getName())));
            }
            fileItemList.add(fileItemDTO);
        }

        return fileItemList;
    }

    @Override
    public String getDownloadUrl(String path) {
        SystemConfig usernameConfig = systemConfigRepository.findByKey(SystemConfigConstant.DOMAIN);
        return StringUtils.removeDuplicateSeparator(usernameConfig.getValue() + "/file/" + getDriveId() + ZFileConstant.PATH_SEPARATOR + path);
    }

    public String getFilePath() {
        return filePath.get();
    }

    @Override
    public StorageTypeEnum getStorageTypeEnum() {
        return StorageTypeEnum.LOCAL;
    }

    @Override
    public FileItemDTO getFileItem(String path) {
        String fullPath = filePath.get() + path;

        File file = new File(fullPath);

        if (!file.exists()) {
            throw new NotExistFileException();
        }

        FileItemDTO fileItemDTO = new FileItemDTO();
        fileItemDTO.setType(file.isDirectory() ? FileTypeEnum.FOLDER : FileTypeEnum.FILE);
        fileItemDTO.setTime(new Date(file.lastModified()));
        fileItemDTO.setSize(file.length());
        fileItemDTO.setName(file.getName());
        fileItemDTO.setPath(filePath.get());
        if (file.isFile()) {
            fileItemDTO.setUrl(getDownloadUrl(path));
        }

        return fileItemDTO;
    }

    @Override
    public List<StorageConfig> storageStrategyConfigList() {
        return new ArrayList<StorageConfig>() {{
            add(new StorageConfig("filePath", "文件路径"));
        }};
    }

    @Override
    public FileItemDTO addFile(@NotNull InputStream inputStream, @NotNull FileItemDTO fileInfo) throws NotSupportedException {
        String path = fileInfo.getPath();
        // 上传时带过来的文件名
        final String initName = fileInfo.getName();
        // 文件最终存储时的名称，不是全路径
        String finalName = initName;
        // 文件存储的文件夹绝对位置
        String fullDirPath = StringUtils.removeDuplicateSeparator(StringUtils.concatUrl(filePath.get(), path));
        // 服务器最终存储的文件绝对地址
        String finalFullPath = StringUtils.concatUrl(fullDirPath,finalName);
        File targetFile  = new File(finalFullPath);
        try {
            // 文件名重复时，自动计数编号
            if(targetFile.exists()) {
                // 文件扩展名称，带圆点符号，例如.txt
                String fileExtention = "";
                String fileName = initName;
                int dotIndex = initName.lastIndexOf(StringUtils.DOT_SPLIT);
                if(dotIndex > 0) {
                    fileExtention = initName.substring(dotIndex);
                    fileName = initName.substring(0, dotIndex);
                }
                int fileNum = 1;
                do {
                    finalName = fileName+StringUtils.LINE_SPLIT+fileNum+fileExtention;
                    finalFullPath = StringUtils.concatUrl(fullDirPath,finalName);
                    targetFile = new File(finalFullPath);
                    fileNum++;
                } while(targetFile.exists());
            }
            byte[] fileData = FileCopyUtils.copyToByteArray(inputStream);
            FileCopyUtils.copy(fileData, targetFile);
            FileItemDTO finalFileInfo = new FileItemDTO();
            finalFileInfo.setName(finalName);
            finalFileInfo.setType(FileTypeEnum.FILE);
            finalFileInfo.setPath(path);
            finalFileInfo.setUrl(getDownloadUrl(StringUtils.concatUrl(path,finalName)));
            return finalFileInfo;
        } catch (Exception e) {
            log.error("文件上传出错", e);
            throw new FileUploadFailException("文件上传失败："+e.getMessage()) ;
        } finally {
            try {
                inputStream.close();                ;
            } catch (IOException e) {
               log.error("文件流关闭异常", e);
            }
        }
    }

    @Override
    public FileItemDTO mkDir(@NotNull FileItemDTO dirInfo) {
        if(FileTypeEnum.FILE.equals(dirInfo.getType())) {
            log.error("创建文件夹失败，FilteItemDTO.type 类型不匹配");
            throw new MakeDirFailException("创建文件夹失败，FilteItemDTO.type 类型不匹配");
        }
        String dirName = dirInfo.getName();
        String path = dirInfo.getPath();
        if(org.apache.commons.lang.StringUtils.isEmpty(path)) {
            log.error("创建文件夹失败，文件夹路径信息为空");
            throw new MakeDirFailException("创建文件夹失败，文件夹路径信息为空");
        }
        if(org.apache.commons.lang.StringUtils.isEmpty(dirName)) {
            log.error("创建文件夹失败，文件夹名称信息为空");
            throw new MakeDirFailException("创建文件夹失败，文件夹名称信息为空");
        }

        // 构造文件夹绝对路径
        String fullDirPath = StringUtils.removeDuplicateSeparator(StringUtils.concatUrl(filePath.get() ,path, dirName));
        // 服务器最终存储的文件绝对地址
        File targetFile  = new File(fullDirPath);
        // 文件名重复时，自动计数编号
        if(targetFile.exists()) {
            log.error("创建文件夹失败，当前目录下存在同名文件夹");
            throw new MakeDirFailException("创建文件夹失败，当前目录下存在同名文件夹");
        }
        targetFile.mkdirs();

        FileItemDTO finalDirInfo = new FileItemDTO();
        finalDirInfo.setName(dirName);
        finalDirInfo.setType(FileTypeEnum.FOLDER);
        finalDirInfo.setPath(path);
        return finalDirInfo;
    }
}