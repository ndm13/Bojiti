package net.miscfolder.bojiti.test.mvc.swing;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ListTableModel<E> extends AbstractTableModel{
	private final List<E> list = new ArrayList<>();
	private final String[] headers;
	private final Class<?>[] types;
	private final Function<E,Object>[] adapters;

	public ListTableModel(String[] headers, Class<?>[] types, Function<E,Object>[] adapters){
		this.headers = Objects.requireNonNull(headers);
		this.types = Objects.requireNonNull(types);
		this.adapters = Objects.requireNonNull(adapters);

		if(headers.length != types.length || types.length != adapters.length)
			throw new IllegalArgumentException("Headers, types, and adapters must be the same length!");
	}

	static <T,R> Function<T,R> f(Function<T,R> f){
		return f;
	}

	public void add(E element){
		list.add(element);
		fireTableDataChanged();
	}

	public void replace(E old, E update){
		synchronized(list){
			int index = list.indexOf(old);
			if(index == -1) return;
			list.set(index, update);
			fireTableRowsUpdated(index, index);
		}
	}

	public void update(E element){
		synchronized(list){
			int index = list.indexOf(element);
			if(index == -1) return;
			fireTableRowsUpdated(index, index);
		}
	}

	public void remove(E element){
		list.remove(element);
		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return list.size();
	}

	@Override
	public int getColumnCount(){
		return headers.length;
	}

	@Override
	public String getColumnName(int columnIndex){
		return headers[columnIndex];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex){
		return types[columnIndex];
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex){
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex){
		return adapters[columnIndex].apply(list.get(rowIndex));
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex){
		throw new UnsupportedOperationException("Model is not editable!");
	}
}
