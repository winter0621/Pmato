package csu.mengya.model;

/**
 * 用户档案实体（对应表 user_profile）。
 *
 * <p>纯数据类，只含字段与 getter/setter。</p>
 *
 * @author 唐天乐
 * @since V1.0
 */
public class UserProfile {

    /** 主键，固定为 1（单用户） */
    private int id;

    /** 昵称 */
    private String nickname;

    /** 累计能量 */
    private int totalEnergy;

    /** 连续打卡天数 */
    private int streakDays;

    /** 创建时间 */
    private String createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getTotalEnergy() {
        return totalEnergy;
    }

    public void setTotalEnergy(int totalEnergy) {
        this.totalEnergy = totalEnergy;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
