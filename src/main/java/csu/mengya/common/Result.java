package csu.mengya.common;

/**
 * 统一业务返回结果（F0 共享地基）。
 *
 * <p>所有 Service 层方法均以此结构返回，避免异常直接穿透到界面层导致崩溃。
 * DAO 抛出的 SQLException 由 Service 捕获后转成 {@link #fail(String)}。</p>
 *
 * @param <T> 业务数据类型
 * @author 唐天乐
 * @since V1.0
 */
public class Result<T> {

    /** 结果码：0 成功，非 0 失败 */
    private final int code;

    /** 失败原因，成功时为空串 */
    private final String message;

    /** 业务数据，失败时为 null */
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 构造成功结果 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "", data);
    }

    /** 构造失败结果 */
    public static <T> Result<T> fail(String message) {
        return new Result<>(1, message, null);
    }

    public boolean isSuccess() {
        return code == 0;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
