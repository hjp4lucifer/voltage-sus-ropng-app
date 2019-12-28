package cn.lucifer.voltagesusropngapp.ui;

/**
 * ui类型
 */
public enum UITypeEnum {
	/**
	 * MenuItem
	 */
	MenuItem(1, "MenuItem"),


	;

	private int code;
	private String name;

	private UITypeEnum(int code, String name) {
		this.code = code;
		this.name = name;
	}

	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
