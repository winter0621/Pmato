package csu.mengya.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 极简同步事件总线（F0 共享地基）。
 *
 * <p>模块之间通过事件通信、互不 import，这是「按模块纵切」能够并行的关键。
 * 订阅方自己处理，发布方不关心谁在听。</p>
 *
 * <p>注意：当前为同步派发，事件在发布线程上依次回调。因为本阶段事件都由
 * 界面线程（按钮点击）发布，所以订阅者能安全地刷新界面；后续若计时线程发布
 * 事件，需要在订阅者内部用 {@link javafx.application.Platform#runLater} 切回。</p>
 *
 * @author 郭艾迪
 * @since V1.0
 */
public final class EventBus {

    /** 事件类型 -> 订阅者列表 */
    private static final Map<Class<?>, List<Consumer<?>>> HANDLERS = new HashMap<>();

    private EventBus() {
    }

    /**
     * 订阅某类事件。
     *
     * @param type    事件类型（具体的事件类）
     * @param handler 处理回调
     * @param <T>     事件类型
     */
    public static <T> void subscribe(Class<T> type, Consumer<T> handler) {
        HANDLERS.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
    }

    /**
     * 发布事件，同步通知所有订阅者。
     *
     * @param event 事件对象
     * @param <T>   事件类型
     */
    @SuppressWarnings("unchecked")
    public static <T> void publish(T event) {
        List<Consumer<?>> handlers = HANDLERS.get(event.getClass());
        if (handlers == null) {
            return;
        }
        for (Consumer<?> handler : handlers) {
            ((Consumer<T>) handler).accept(event);
        }
    }
}
