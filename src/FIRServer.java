import java.io.*;
import java.net.*;
import java.nio.charset.MalformedInputException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;

public class FIRServer extends JFrame implements FIRConstants{

	static int port=8000;
 public static void main(String[] args){
	 FIRServer frame= new FIRServer(); 
 }
 
 public FIRServer() {
	// TODO Auto-generated constructor stub
	 JTextArea jtaLog = new JTextArea();
	 
	 JScrollPane scrollPane = new JScrollPane(jtaLog);
	 add(scrollPane,BorderLayout.CENTER);
	 
	 setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	 setSize(300,300);
	 setTitle("Five-In-A-ROW");
	 setVisible(true);
	 
	 try {
		//create a server socket
		 ServerSocket serverSocket = new ServerSocket(port);
		 jtaLog.append(new Date()+":Server started at socket"+ port+ "\n");
		 
		 //number a session
		 int sessionNo=1;
		 
		 //ready to create a session for every two players
		 while(true){
			 jtaLog.append(new Date()+":等待玩家加入游戏室"+ sessionNo +'\n');
			 
			 //Connect to player1
			 Socket player1 = serverSocket.accept();
			 
			 jtaLog.append(new Date()+":玩家1加入游戏室"+ sessionNo + '\n' );
			 jtaLog.append("玩家1的IP地址是"+player1.getInetAddress().getHostAddress()+ '\n');
			 
			 //Notify that the player is Player1
			 new DataOutputStream(player1.getOutputStream()).writeInt(PlAYER1);
			 
			 //Connect to player2
			 Socket player2 = serverSocket.accept();
			 
			 jtaLog.append(new Date()+":玩家2加入游戏室"+ sessionNo + '\n' );
			 jtaLog.append("玩家2的IP地址是"+player2.getInetAddress().getHostAddress()+ '\n');
			 
			 //Notify that the player is Player2
			 new DataOutputStream(player2.getOutputStream()).writeInt(PlAYER2);
			 
			 //Display this session and increment session number
			 jtaLog.append(new Date()+":正在启动线程-游戏室" + sessionNo++ + '\n');
			 
			 //Create a new thread for this session of two players
			 HandleAGame task = new HandleAGame(player1, player2);
			 
			 //Start the new thread
			  new Thread(task).start();
			 
		 }
	} catch (IOException e) {
		System.out.println("FIRServer() error:" + e.getMessage());
	}
	 
}
}

class HandleAGame implements Runnable,FIRConstants{
	private Socket player1;
	private Socket player2;
	
	//Create and initialize cells
	private char[][] chessman= new char[NUMBER][NUMBER];
	private DataInputStream fromPlayer1;
	private DataInputStream fromPlayer2;
	private DataOutputStream toPlayer1;
	private DataOutputStream toPlayer2;
	
	//Continue to play
	private boolean continueToPlay = true;
	
	/*Construct a thread*/
	public HandleAGame(Socket player1,Socket player2) {
		this.player1 = player1;
		this.player2 = player2;
		
		//Initialize cells
		for(int i = 0; i < NUMBER ; i++)
			for(int j = 0 ; j < NUMBER ; j++)
				chessman[i][j]=' ';
		
 	}

/*Implement the run() method for the thread*/
	public void run(){
		try {
			//Create data input and output streams
			DataInputStream fromPlayer1 = new DataInputStream(player1.getInputStream());
			DataInputStream fromPlayer2 = new DataInputStream(player2.getInputStream());
			DataOutputStream toPlayer1 = new DataOutputStream(player1.getOutputStream());
			DataOutputStream toPlayer2 = new DataOutputStream(player2.getOutputStream());
			
			//write anything to notify player1 to start
			//This is just to let player 1 know to start
			toPlayer1.writeInt(1);
			
			//Continuously serve the players and determine and report 
			//The game status to the players
			while(true){
			//Receive a move from player1
				int row = fromPlayer1.readInt();
				int column = fromPlayer1.readInt();
				chessman[row][column] = 'B';
				
				//Check if player 1 wins
				if (isWon('B')) {
					toPlayer1.writeInt(PlAYER1_WON);
					toPlayer2.writeInt(PlAYER1_WON);
					sendMove(toPlayer2, row, column);
					break;
				}
				else if(isFull()){//Check if all cells are filled
					toPlayer1.writeInt(DRAW);
					toPlayer2.writeInt(DRAW);
					sendMove(toPlayer2, row, column);
					break;
				}
				else {
					//Notify player2 to take the turn 
					toPlayer2.writeInt(CONTINUE);
					
					//Send player 1's selected row and column to player2
					sendMove(toPlayer2, row, column);
				}
				
				//Receive a move from player2
				row = fromPlayer2.readInt();
				column = fromPlayer2.readInt();
				chessman[row][column]='W';
				
				//Check if player2 wins
				if (isWon('W')) {
					toPlayer1.writeInt(PlAYER2_WON);
					toPlayer2.writeInt(PlAYER2_WON);
					sendMove(toPlayer1, row, column);
					break;
				}
				else{
					// Notify player 1 to take the turn
			          toPlayer1.writeInt(CONTINUE);

			          // Send player 2's selected row and column to player 1
			          sendMove(toPlayer1, row, column);
				}
				
			}
			} catch (IOException e) {
				System.out.println("run() Error:" + e.getMessage());
	}
}

/* Send the move to other player*/
private void sendMove(DataOutputStream out,int row,int column)throws IOException{
	out.writeInt(row);//Send row index
	out.writeInt(column);//Send column index
}

/*Determine if the player with the specified token wins*/
private boolean isWon(char token){
	
	//Check rows
	for(int i=0;i<NUMBER;i++)
		for(int j=0; j<NUMBER-4;j++)
		 if (chessman[i][j]== token && chessman[i][j+1]== token && 
		 		chessman[i][j+2]== token && chessman[i][j+3]== token && chessman[i][j+4]== token) 
			 return true;
	//Check columns
	for(int i = 0 ; i < NUMBER ; i++)
		for(int j = 0 ; j < NUMBER-4 ; j++)
		 if (chessman[j][i]== token && chessman[j+1][i]== token && 
		 		chessman[j+2][i]== token && chessman[j+3][i]== token && chessman[j+4][i]== token) 
			 return true;
	//Check top-left to bottom right
	for(int i = 0 ; i < NUMBER-4 ; i++)
		for(int j = 0 ; j < NUMBER-4 ; j++)
			if (chessman[i][j]==token && chessman[i+1][j+1]==token && 
					chessman[i+2][j+2]==token && chessman[i+3][j+3]==token && chessman[i+4][j+4]==token) 
				return true;
	//Check top-right to bottom-left
	for(int i = NUMBER-1 ; i > 3 ; i--)
		for(int j = 0 ; j < NUMBER-4 ; j++)
			if (chessman[i][j]==token && chessman[i-1][j+1]==token && 
					chessman[i-2][j+2]==token && chessman[i-3][j+3]==token && chessman[i-4][j+4]==token) 
				return true;
	
	//All check but no winner
	return false;
}

private boolean isFull(){
	for(int i = 0 ; i < NUMBER ; i++)
		for(int j = 0 ; j< NUMBER ; j++)
			if (chessman[i][j] ==' ') return false;
	return true;
}

}
