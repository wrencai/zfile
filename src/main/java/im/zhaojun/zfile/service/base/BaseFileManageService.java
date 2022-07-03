package im.zhaojun.zfile.service.base;

import im.zhaojun.zfile.model.dto.DriveConfigDTO;
import im.zhaojun.zfile.model.dto.FileItemDTO;

import javax.transaction.NotSupportedException;
import java.io.InputStream;

/**
 * 文件管理接口定义
 * @author w23280
 * @date 2022/6/30 18:04
 */
public interface BaseFileManageService {

    /**
     * 新增文件到指定驱动器的指定位置
     * @param inputStream 文件流
     * @param fileInfo 上传的文件信息
     * @return 上传后的文件信息说明
     * @throws NotSupportedException 如果文件系统不支持，则抛出次异常
     */
    FileItemDTO addFile(InputStream inputStream, FileItemDTO fileInfo) throws NotSupportedException;

    /**
     * 新建文件夹
     * @param dirInfo
     * @return 文件夹信息
     * @throws NotSupportedException 如果文件系统不支持，则抛出次异常
     */
    FileItemDTO mkDir(FileItemDTO dirInfo) throws NotSupportedException;

    /**
     * 删除文件或者文件夹
     * @param fileInfo
     * @return 删除的文件或者文件夹信息
     * @throws NotSupportedException 如果文件系统不支持，则抛出次异常
     */
    FileItemDTO delete(FileItemDTO fileInfo) throws NotSupportedException;

    /**
     * 移动文件或者文件夹位置，如果移动的路径相同，则可以用来进行重命名。
     * @param srcFile
     * @param targetFile
     * @return 移动后的文件或者文件夹信息
     * @throws NotSupportedException 如果文件系统不支持，则抛出次异常
     */
    FileItemDTO move(FileItemDTO srcFile, FileItemDTO targetFile) throws NotSupportedException;
}
