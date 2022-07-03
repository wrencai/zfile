package im.zhaojun.zfile.service.base;

import im.zhaojun.zfile.model.dto.FileItemDTO;

import javax.transaction.NotSupportedException;
import java.io.InputStream;

/**
 * 文件管理类抽象类，默认不支持管理操作。
 * @author wrc
 */
public class AbstractBaseFileManageService implements  BaseFileManageService {
    @Override
    public FileItemDTO addFile(InputStream inputStream, FileItemDTO fileInfo) throws NotSupportedException {
        throw new NotSupportedException("驱动器暂不支持此操作");
    }

    @Override
    public FileItemDTO mkDir(FileItemDTO dirInfo) throws NotSupportedException {
        throw new NotSupportedException("驱动器暂不支持此操作");
    }

    @Override
    public FileItemDTO delete(FileItemDTO fileInfo)  throws NotSupportedException {
        throw new NotSupportedException("驱动器暂不支持此操作");
    }

    @Override
    public FileItemDTO move(FileItemDTO srcFile, FileItemDTO targetFile)  throws NotSupportedException {
        throw new NotSupportedException("驱动器暂不支持此操作");
    }
}
