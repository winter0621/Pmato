package csu.mengya.model;

/**
 * 作物图鉴实体（对应表 plant_species）。
 *
 * <p>纯数据类，只含字段与 getter/setter，不含业务逻辑。</p>
 *
 * @author B
 * @since V1.0
 */
public class PlantSpecies {

    /** 物种 ID，如 'sunflower' */
    private String id;

    /** 名称，如 '向日葵' */
    private String name;

    /** 稀有度：common | rare | epic */
    private String rarity;

    /** 解锁所需累计能量门槛 */
    private int unlockEnergy;

    /** 稀有度系数（影响生长阈值缩放） */
    private double stageScale;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public int getUnlockEnergy() {
        return unlockEnergy;
    }

    public void setUnlockEnergy(int unlockEnergy) {
        this.unlockEnergy = unlockEnergy;
    }

    public double getStageScale() {
        return stageScale;
    }

    public void setStageScale(double stageScale) {
        this.stageScale = stageScale;
    }
}
