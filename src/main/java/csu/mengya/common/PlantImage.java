package csu.mengya.common;

import javafx.scene.image.Image;

import java.io.InputStream;

/**
 * 植物贴图加载器（F2 植物园美术资源）。
 *
 * <p>从 classpath 的 images/plants/ 目录加载真实贴图，文件命名约定为
 * {@code {speciesId}_stage{stage}.png}，例如 {@code sunflower_stage4.png}。</p>
 *
 * <p>依据 PlantEmoji 中的预留方案：界面代码只与本类打交道，
 * 后续新增物种或更换图片时只需替换 resources/images/plants/ 下的资源文件，
 * 界面代码无需改动。加载失败的场景返回 null，由调用方自行回退占位。</p>
 *
 * @author B
 * @since V1.0
 */
public final class PlantImage {

    /** 图片资源根路径（classpath 相对路径，resources 目录为根） */
    private static final String ROOT = "/images/plants/";

    private PlantImage() {
    }

    /**
     * 加载某物种某生长阶段的贴图。
     *
     * @param speciesId 物种 ID，如 'sunflower'
     * @param stage     生长阶段 0-4（0 种子 / 1 幼苗 / 2 抽枝 / 3 花苞 / 4 成熟）
     * @return 对应贴图；资源不存在或读取失败时返回 null
     */
    public static Image of(String speciesId, int stage) {
        String path = ROOT + speciesId + "_stage" + stage + ".png";
        try (InputStream in = PlantImage.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return new Image(in);
        } catch (Exception e) {
            // 资源缺失不应导致界面崩溃，交由调用方兜底
            return null;
        }
    }

    /**
     * 加载某物种成熟期（阶段 4）贴图，用于图鉴列表缩略展示。
     *
     * @param speciesId 物种 ID
     * @return 成熟期贴图；缺失时返回 null
     */
    public static Image matureOf(String speciesId) {
        return of(speciesId, 4);
    }
}
