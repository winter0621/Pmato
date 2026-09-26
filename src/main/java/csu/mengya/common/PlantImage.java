package csu.mengya.common;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 植物贴图加载器（F2 植物园美术资源）。
 *
 * <p>从 classpath 的 images/plants/ 目录加载真实贴图，文件命名约定为
 * {@code {speciesId}_stage{stage}.png}，例如 {@code sunflower_stage4.png}。</p>
 *
 * <p>三个关键优化（针对大图导致的卡顿）：</p>
 * <ol>
 *   <li>按显示尺寸解码：用目标尺寸构造 Image，避免解码原图分辨率；</li>
 *   <li>图片缓存：同一张图只解码一次；</li>
 *   <li>后台异步加载：不阻塞 JavaFX 界面线程。</li>
 * </ol>
 *
 * <p>加载失败的场景返回 null，由调用方回退 Emoji 占位。</p>
 *
 * @author B
 * @since V1.0
 */
public final class PlantImage {

    /** 图片资源根路径（classpath 相对路径，resources 目录为根） */
    private static final String ROOT = "/images/plants/";

    /** 图片缓存：key(路径@尺寸) -> 已加载的图片 */
    private static final Map<String, Image> CACHE = new HashMap<>();

    private PlantImage() {
    }

    /**
     * 加载某物种某生长阶段的贴图（地块显示，按 128px 解码）。
     *
     * @param speciesId 物种 ID，如 'sunflower'
     * @param stage     生长阶段 0-4
     * @return 对应贴图；资源不存在时返回 null
     */
    public static Image of(String speciesId, int stage) {
        return load(speciesId, stage, 128);
    }

    /**
     * 加载某物种成熟期（阶段 4）贴图（图鉴缩略图，按 40px 解码）。
     *
     * @param speciesId 物种 ID
     * @return 成熟期贴图；缺失时返回 null
     */
    public static Image matureOf(String speciesId) {
        return load(speciesId, 4, 40);
    }

    /** 统一加载入口：按目标尺寸解码 + 缓存 + 后台加载 */
    private static Image load(String speciesId, int stage, int size) {
        String path = ROOT + speciesId + "_stage" + stage + ".png";
        String key = path + "@" + size;
        Image cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        URL url = PlantImage.class.getResource(path);
        if (url == null) {
            return null;
        }
        // requestedWidth/Height=size：解码时直接缩到目标尺寸，避免解码大图；
        // 最后一个 true = 后台异步加载，立即返回不卡界面。
        Image img = new Image(url.toExternalForm(), size, size, true, true, true);
        CACHE.put(key, img);
        return img;
    }
}
