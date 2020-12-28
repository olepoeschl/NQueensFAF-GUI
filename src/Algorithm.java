

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
		for (int col = 0; col < N; col++)                       //f�r jede Spalte in der �bergebenen Zeile (Zelle), wird folgendes ausgef�hrt
		{
			if(row == 0)
            {
				System.out.println("1. Dame auf Feld " + (col+1) + " / " + N);
            }

			if (SqareIsSafe(row, col))                          //Ist die �bergebene Zelle nicht gedeckt...
			{            
				field[row][col] = true;							//...wird der Wert der Zelle auf true gesetzt ( = Dame)
				trycounter++;                                   //Anzahl der Damensetzungen wird um 1 erh�ht
				if (row < N-1)
                {
					SetQueen(row + 1);
                }
                else											//neue L�sung gefunden
                {
//					PrintField();
					solvecounter++;								
                }

				field[row][col] = false;						//setze Dame zur�ck
			}
			if (row > 1 && col == N - 1)						//Wenn angekommen bei Feld der letzten Spalte...
            {	
				break; 											//...dann kehre zur�ck zum n�chst h�hergelegenen Stackframe (zum vorherigen SetQueen()-Aufruf)
            }			                                  
		}
	}
	private boolean SqareIsSafe(int r, int c)					//Pr�ft ob das �bergebene Feld von einer anderen Dame gedeckt ist.
	{
		for (int i = 0; i < r; i++)								// Wesentliche Algorithmusbeschleunigung durch entfernung der horizontalpr�fung
		{														// und beschleunigung der Vertikalpr�fung, diese muss n�mlich nur nach oben aber nicht nach unten ausgef�hrt werden
				if( field[i][c] )
				{
					return false;
				}
		}
		for (int i = r; i > 0; i--)                             //Pr�ft die Diagonalen links nach oben und rechts nach oben 
		{                                                       //Die anderen �berpr�fungen w�ren m�glich aber �berfl�ssig
			if (c >= i)                                         //if (c >= i) und if (c + i < N) m�ssen getrennt �berpr�ft werden. 
			{                                                   //Wird die eine Abbruchbedingung ausgef�hrt, wird die andere Diagonale nicht mehr gepr�ft
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

		}														//Um zu Pr�fen ob ein Feld von einer Dame gedeckt ist, m�ssten theoretisch alle Richtungen der Dame gepr�ft werden. da aber 
																//In jede Zeile Grunds�tzlich nur eine Dame gesetzt wird, muss die Horizontale nicht gepr�ft werden.
																//Da wir au�erdem die Damen von Oben nach Unten setzen, kann (w�hrend dieser Pr�fung) keine Dame Diagonal oder Vertikal unter einer anderen stehen
																//Dadurch m�ssen nur 3/8 Pr�fungen ausgef�hrt werden. Und es kann dennoch keine Fehler geben.

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
		
		System.out.println("[" + ( time/1000/60 ) + ":" + (time/1000%60) + "." + (time%1000) + "] \t" + alg.getTrycounter() + " Versuche, " + alg.getSolvecounter() + " L�sungen f�r N = " + N);
		
		/*
		 * [5:40.729] f�r N = 16
		 * 
		 * 
		 */
		
	}

}
