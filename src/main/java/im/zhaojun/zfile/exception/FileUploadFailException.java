package im.zhaojun.zfile.exception;

/**
 * 文件上传失败异常
 * @author w23280
 * @date 2022/6/30 19:59
 */
public class FileUploadFailException extends RuntimeException {
    private String msg;

    public FileUploadFailException(String msg) {
        this.msg = msg;
    }

    @Override
    public String getMessage() {
        return msg;
    }
}
