package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;

import calc.BoardProperties;

public class FAFProcessData extends ArrayDeque<BoardProperties> implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public int N;
	public long solvecounter, startConstCount, calculatedStartConstCount, time;
	
	public void save(String filename) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
			out.writeObject(this);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static FAFProcessData load(String filename) {
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(filename));
			return (FAFProcessData) in.readObject();
			
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
