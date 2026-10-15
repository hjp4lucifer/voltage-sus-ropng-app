package cn.lucifer.voltagesusropngapp.adt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import cn.lucifer.voltage.sus.dict.CharaConst;
import cn.lucifer.voltagesusropngapp.R;

/**
 * 角色选择列表适配器：每行两列——序号列（{@code chara_id}）+ 名字列。
 * <p>
 * 数据源为 {@link CharaConst#charaNames()} 的 key（{@code chara_id} 升序），名字由 {@code chara_id} 反查得到；
 * 展示串（序号 + 名字）与回填值（仅名字）必须解耦，回填逻辑见调用方。
 */
public class CharaChoiceAdapter extends BaseAdapter {

	protected Context context;

	/**
	 * 角色 id 列表，顺序即展示顺序（{@code chara_id} 升序），同时用于把列表位置映射回 {@code chara_id}
	 */
	protected List<Integer> charaIdList;

	public CharaChoiceAdapter(Context context, List<Integer> charaIdList) {
		this.context = context;
		this.charaIdList = charaIdList;
	}

	@Override
	public int getCount() {
		return charaIdList.size();
	}

	@Override
	public Integer getItem(int position) {
		return charaIdList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return charaIdList.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			rowView = LayoutInflater.from(context).inflate(R.layout.item_chara_choice, parent, false);
		}

		Integer charaId = charaIdList.get(position);
		String characterName = CharaConst.nameOf(charaId);

		TextView textCharaId = rowView.findViewById(R.id.text_chara_id);
		TextView textCharaName = rowView.findViewById(R.id.text_chara_name);
		textCharaId.setText(String.valueOf(charaId));
		textCharaName.setText(characterName == null ? "" : characterName);

		return rowView;
	}
}
