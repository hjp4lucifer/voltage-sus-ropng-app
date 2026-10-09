package cn.lucifer.voltagesusropngapp.model;

/**
 * 配置文件的数据载体，即 {@link cn.lucifer.voltagesusropngapp.util.AppSettings} 中可导入导出的子集。
 * <p>
 * 命名取「配置文件」而非 Config/Settings，以区别于内存态的 AppSettings；字段名即 JSON key；
 * 可空字段使用包装类型，使"文件缺失该字段"能表达为 null，从而与导入端"缺失则不覆盖 / 清空"的既有行为保持一致。
 */
public class ConfigFile {

	private String nsid;

	private String deviceUid;

	private Integer pfid;

	private String puKey;

	private Boolean rookie;

	private Integer arenaId;

	private String raidId;

	private String appliVersion;

	private Integer sleepBaseMs;

	private String cardUpgradeIdList;

	public String getNsid() {
		return nsid;
	}

	public void setNsid(String nsid) {
		this.nsid = nsid;
	}

	public String getDeviceUid() {
		return deviceUid;
	}

	public void setDeviceUid(String deviceUid) {
		this.deviceUid = deviceUid;
	}

	public Integer getPfid() {
		return pfid;
	}

	public void setPfid(Integer pfid) {
		this.pfid = pfid;
	}

	public String getPuKey() {
		return puKey;
	}

	public void setPuKey(String puKey) {
		this.puKey = puKey;
	}

	public Boolean getRookie() {
		return rookie;
	}

	public void setRookie(Boolean rookie) {
		this.rookie = rookie;
	}

	public Integer getArenaId() {
		return arenaId;
	}

	public void setArenaId(Integer arenaId) {
		this.arenaId = arenaId;
	}

	public String getRaidId() {
		return raidId;
	}

	public void setRaidId(String raidId) {
		this.raidId = raidId;
	}

	public String getAppliVersion() {
		return appliVersion;
	}

	public void setAppliVersion(String appliVersion) {
		this.appliVersion = appliVersion;
	}

	public Integer getSleepBaseMs() {
		return sleepBaseMs;
	}

	public void setSleepBaseMs(Integer sleepBaseMs) {
		this.sleepBaseMs = sleepBaseMs;
	}

	public String getCardUpgradeIdList() {
		return cardUpgradeIdList;
	}

	public void setCardUpgradeIdList(String cardUpgradeIdList) {
		this.cardUpgradeIdList = cardUpgradeIdList;
	}
}
