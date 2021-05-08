package calc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class ProgressBackup implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// variables
	public int N;
	public long solvecounter, time;
	public int startConstCount;
	public ArrayDeque<Integer> remainingConstellations;
	public ArrayList<Object> customValues;					// for custom variables (e.g. IDs)
	
	// constructor
	public ProgressBackup(Solver solver) {
		N = solver.getN();
		solvecounter = solver.getSolvecounter();
		time = solver.getFTime();
		startConstCount = solver.getStartConstCount();
		remainingConstellations = solver.getUnsolvedStartConstellations();
		
		customValues = new ArrayList<Object>();
	}
	
	public void addExtraValue(Object var) {
		customValues.add(var);
	}
	
	public void save(String path) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
			out.writeObject(this);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ProgressBackup restore(String path) {
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(path));
			return (ProgressBackup) in.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
