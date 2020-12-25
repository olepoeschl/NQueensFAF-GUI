package mainpackage;

import java.util.ArrayList;
import java.util.Date;

public class AlgorithmStarter_alt {

	private int N;												//Brettgröße
	private int cpu;											//Anzahl der gewünschten Threads (Anzahl der Kerne)
	private boolean pausable;									//Soll der Algorithmus-Durchlauf pausierbar sein?
	private boolean paused = false, finished = false;
	long trycounter = 0, solvecounter = 0;
	String timestr;
	
	public AlgorithmStarter_alt(int N, int cpu, boolean pausable) {
		this.N = N;
		this.cpu = cpu;
		this.pausable = pausable;
	}
	
	public void startAlgorithm() {
		
		finished = false;
		
		int halfN = (N + (N % 2)) / 2;							//bis zu welcher Position der ersten Dame muss man rechnen?
		int posCount = (int)( halfN / cpu);						//Anzahl der Felder, die je Thread mindestens abgearbeitet werden müssen
		int posCountRest = halfN % cpu;							//Antzahl der Threads, die 1 Feld mehr abarbeiten müssen als die anderen Threads
		int colMin = 0;
		int prioHigh = 7, prioNormal = 5;
		
		ArrayList<AlgorithmThread_alt> threadlist = new ArrayList<AlgorithmThread_alt>();
		
		//Pausable-Flag setzen
		AlgorithmThread_alt.setPausable(pausable);
		
		int id = 1;
		do {
			if(posCountRest > 0) {
				AlgorithmThread_alt algThread = new AlgorithmThread_alt(N, halfN, colMin, colMin + posCount, id);
				algThread.setPriority(prioHigh);
				threadlist.add(algThread);
				
//				System.out.println("AlgorithmThread_alt " + id + " wurde erstellt mit colMin = " + colMin + ", colMax = " + (colMin + posCount) + " und priority = " + prioHigh);
				
				colMin = colMin + posCount + 1;
				posCountRest--;
			} else {
				AlgorithmThread_alt algThread = new AlgorithmThread_alt(N, halfN, colMin, colMin + posCount - 1, id);
				algThread.setPriority(prioNormal);
				threadlist.add(algThread);
				
//				System.out.println("AlgorithmThread_alt " + id + " wurde erstellt mit colMin = " + colMin + ", colMax = " + (colMin + posCount - 1) + " und priority = " + prioNormal);
				
				colMin = colMin + posCount;
			}
			
			id++;
		} while(colMin < halfN);
		
		
		//Startzeit erfassen
		long start = new Date().getTime();
		
		//Algorithmus-Threads starten
		for(AlgorithmThread_alt algThread : threadlist) {
			algThread.start();
		}
		
		//solange es laufende Threads gibt, pausiere Sie ggf und überprüfe ob es noch welche gibt
		ArrayList<AlgorithmThread_alt> runningThreads = new ArrayList<AlgorithmThread_alt>();
		for(AlgorithmThread_alt algThread : threadlist) {
			runningThreads.add(algThread);
		}
		while(runningThreads.size() > 0) {
			for(AlgorithmThread_alt algThread : runningThreads) {
				if( ! algThread.isAlive() ) {
					runningThreads.remove(algThread);
					break;
				}
			}
			Thread.yield();							//Thread signalisiert dasss er gerade nicht wichtig ist und dadurch andere Threads mehr Prozessorzeit in Anspruch nehemn können 
		}
//			if(paused) {
//				AlgorithmThread_alt.hold();
//				while(paused) {
//					try {
//						Thread.sleep(50);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//				AlgorithmThread_alt.go();
//			}
			
//			try {
//				Thread.sleep(50);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		
		//Auf Algorithmus-Threads warten
//		for(AlgorithmThread_alt algThread : threadlist) {
//			try {
//				algThread.join();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		
		finished = true;
		
		//Endzeit erfassen
		long end = new Date().getTime();
		long pausetime = threadlist.get(0).getPausetime();
		long time = end - start - pausetime;
		System.out.println("pausetime = " + pausetime);
		
		for(AlgorithmThread_alt algThread : threadlist) {
			trycounter += algThread.getTrycounter();
			solvecounter += algThread.getSolvecounter();
		}
		
		if(N % 2 == 0) {
			solvecounter *= 2;
		} else {
			solvecounter = solvecounter * 2 - AlgorithmThread_alt.getMidcounter();
		}
		
		timestr = "[" + ( time/1000/60 ) + ":" + (time/1000%60) + "." + (time%1000) + "]";
		System.out.println(timestr + "\t" + trycounter + " Versuche, " + solvecounter + " Lösungen für N = " + N);
	}

	public void hold() {
		paused = true;
		AlgorithmThread_alt.hold();
	}
	public void go() {
		paused = false;
		AlgorithmThread_alt.go();
	}
	public boolean isPaused() {
		return paused;
	}
	public boolean isFinished() {
		return finished;
	}
	
	public long getTrycounter() {
		return trycounter;
	}
	public long getSolvecounter() {
		return solvecounter;
	}
	public String getTime() {
		return timestr;
	}
}