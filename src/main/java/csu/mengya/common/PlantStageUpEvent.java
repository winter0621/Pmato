package csu.mengya.common;

/**
 * 植物生长阶段升级事件（F2 植物园发布，F5 统计订阅）。
 *
 * @author 唐天乐
 * @since V1.0
 */
public class PlantStageUpEvent {

    private final int plotId;
    private final String speciesId;
    private final int newStage;

    public PlantStageUpEvent(int plotId, String speciesId, int newStage) {
        this.plotId = plotId;
        this.speciesId = speciesId;
        this.newStage = newStage;
    }

    public int getPlotId() {
        return plotId;
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public int getNewStage() {
        return newStage;
    }
}
