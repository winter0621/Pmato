package csu.mengya.model;

/**
 * 植物园地块实体（对应表 garden_plot）。
 *
 * <p>纯数据类，只含字段与 getter/setter，不含业务逻辑。</p>
 *
 * @author 唐天乐
 * @since V1.0
 */
public class GardenPlot {

    /** 主键 */
    private int id;

    /** 种植的物种 ID */
    private String speciesId;

    /** 园内位置 */
    private int slotIndex;

    /** 种植时间（ISO8601） */
    private String plantedAt;

    /** 累计能量 EP */
    private int energy;

    /** 生长阶段 0-4 */
    private int stage;

    /** 状态：growing | mature | harvested */
    private String status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(String speciesId) {
        this.speciesId = speciesId;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public String getPlantedAt() {
        return plantedAt;
    }

    public void setPlantedAt(String plantedAt) {
        this.plantedAt = plantedAt;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
