package im.zhaojun.zfile.exception;

/**
 * 创建文件夹失败异常
 * @author w23280
 * @date 2022/7/1 10:47
 */
public class MakeDirFailException  extends RuntimeException {
    public MakeDirFailException(String msg) {
        super(msg);
    }
}
