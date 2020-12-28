

import java.util.Date;

public class Algorithm {
	
	//Globale Variablen
	int N;
	public int trycounter, solvecounter;
	boolean[][] field;	

	public Algorithm(int N)
	{															//Einfacher Algorithm Konstruktor
		this.N = N;
		field = new boolean[N][N];
	}
	public void StartAlgorithm()								//Aufruf Funktion des Algorithmus
	{
		Initializefield();                                      //Das gesamte Feld wird auf false gesetzt
		//PrintField();                                         //gib initialisiertes Feld aus
		SetQueen(0);											//rekursiver Algorithmus wird aufgerufen
	}

	private void SetQueen(int row)								//Konzept zur Schrittweise Abarbeitung
	{
		for (int col = 0; col < N; col++)                       //für jede Spalte in der übergebenen Zeile (Zelle), wird folgendes ausgeführt
		{
			if(row == 0)
            {
				System.out.println("1. Dame auf Feld " + (col+1) + " / " + N);
            }

			if (SqareIsSafe(row, col))                          //Ist die übergebene Zelle nicht gedeckt...
			{            
				field[row][col] = true;							//...wird der Wert der Zelle auf true gesetzt ( = Dame)
				trycounter++;                                   //Anzahl der Damensetzungen wird um 1 erhöht
				if (row < N-1)
                {
					SetQueen(row + 1);
                }
                else											//neue Lösung gefunden
                {
//					PrintField();
					solvecounter++;								
                }

				field[row][col] = false;						//setze Dame zurück
			}
			if (row > 1 && col == N - 1)						//Wenn angekommen bei Feld der letzten Spalte...
            {	
				break; 											//...dann kehre zurück zum nächst höhergelegenen Stackframe (zum vorherigen SetQueen()-Aufruf)
            }			                                  
		}
	}
	private boolean SqareIsSafe(int r, int c)					//Prüft ob das übergebene Feld von einer anderen Dame gedeckt ist.
	{
		for (int i = 0; i < r; i++)								// Wesentliche Algorithmusbeschleunigung durch entfernung der horizontalprüfung
		{														// und beschleunigung der Vertikalprüfung, diese muss nämlich nur nach oben aber nicht nach unten ausgeführt werden
				if( field[i][c] )
				{
					return false;
				}
		}
		for (int i = r; i > 0; i--)                             //Prüft die Diagonalen links nach oben und rechts nach oben 
		{                                                       //Die anderen überprüfungen wären möglich aber überflüssig
			if (c >= i)                                         //if (c >= i) und if (c + i < N) müssen getrennt überprüft werden. 
			{                                                   //Wird die eine Abbruchbedingung ausgeführt, wird die andere Diagonale nicht mehr geprüft
				if (field[r - i][c - i])
				{	
					return false;
				}
			}
			if (c + i < N)
			{
				if (field[r - i][c + i])
				{
					return false;
				}
			}

		}														//Um zu Prüfen ob ein Feld von einer Dame gedeckt ist, müssten theoretisch alle Richtungen der Dame geprüft werden. da aber 
																//In jede Zeile Grundsätzlich nur eine Dame gesetzt wird, muss die Horizontale nicht geprüft werden.
																//Da wir außerdem die Damen von Oben nach Unten setzen, kann (während dieser Prüfung) keine Dame Diagonal oder Vertikal unter einer anderen stehen
																//Dadurch müssen nur 3/8 Prüfungen ausgeführt werden. Und es kann dennoch keine Fehler geben.

		return true;
	}
	
	private void PrintField()									//Funktion zur Ausgabe des Feldes
	{
		System.out.println();
		for (int i = 0; i < N; i++)
		{
			for (int j = 0; j < N; j++)
			{
				if (field[i][j])
                {
					System.out.print("O ");
                }
                else
                {
					System.out.print("x ");
                }	
			}
			System.out.println();
		}
	}
	private void Initializefield()								//Initialisiert field ... setzt alle Speicherinhalte auf x
	{
		for (int i = 0; i < N; i++)
		{
			for (int j = 0; j < N; j++)
			{
				field[i][j] = false;
			}
		}
	}

	public int getTrycounter() {
		return trycounter;
	}
	public int getSolvecounter() {
		return solvecounter;
	}
	
	//---
	public static void main(String[] args) {
		
		int N = 16;
		Algorithm alg = new Algorithm(N);
		
		
		long start = new Date().getTime();
		alg.StartAlgorithm();
		long end = new Date().getTime();
		long time = end - start;
		
		System.out.println("[" + ( time/1000/60 ) + ":" + (time/1000%60) + "." + (time%1000) + "] \t" + alg.getTrycounter() + " Versuche, " + alg.getSolvecounter() + " Lösungen für N = " + N);
		
		/*
		 * [5:40.729] für N = 16
		 * 
		 * 
		 */
		
	}

}
