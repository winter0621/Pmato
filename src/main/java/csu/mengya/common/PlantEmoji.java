package csu.mengya.common;

/**
 * 植物 Emoji 占位（F2 美术占位）。
 *
 * <p>依据计划书 8.2 节预案：美术资源来不及，先用 Emoji + 缩放动画占位，功能完整优先。
 * 界面代码只与本类打交道；将来换成 images/plants/ 下的真实图片时，只需改造本类
 * （新增一个 PlantImage），界面代码无需改动。</p>
 *
 * @author 唐天乐
 * @since V1.0
 */
public final class PlantEmoji {

    private PlantEmoji() {
    }

    /**
     * 物种 ID -> Emoji 字符。
     *
     * @param speciesId 物种 ID，如 'sunflower'
     * @return 对应 Emoji，未知物种返回幼苗占位 🌱
     */
    public static String of(String speciesId) {
        switch (speciesId) {
            case "sunflower":
                return "🌻";
            case "tomato":
                return "🍅";
            case "cactus":
                return "🌵";
            case "tulip":
                return "🌷";
            case "rose":
                return "🌹";
            case "lily":
                return "🌼";
            case "orchid":
                return "🌺";
            case "sakura":
                return "🌸";
            default:
                return "🌱";
        }
    }

    /**
     * 生长阶段 -> Emoji 字号（像素）。
     * 用字号大小表现生长：种子小、成熟大，一眼看出「长大了」。
     *
     * @param stage 生长阶段 0-4
     * @return 对应字号
     */
    public static double fontSize(int stage) {
        return 20 + stage * 8;   // 20 / 28 / 36 / 44 / 52
    }
}
