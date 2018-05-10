import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RGBImageFilter;

import javax.naming.NamingEnumeration;
import javax.swing.*;
import javax.swing.border.LineBorder;

import org.w3c.dom.css.RGBColor;


import java.io.*;
import java.net.*;
import java.security.cert.PolicyQualifierInfo;

public class FIRClient extends JApplet implements Runnable,FIRConstants {

	private int port = 8000;
	//Indicate whether the player has the turn
	private boolean myTurn = false;
	
	//Indicate the token for the player/other player
	private char myToken = ' ';
	private char otherToken = ' ';
	
	//Create and Initialize the cells
	private Chessman[][] chessman = new Chessman[NUMBER][NUMBER];
	
	//Create and Initialize a title label
	private JLabel jlblTitle = new JLabel();
	
	//Create and Initialize a status label
	private JLabel jlblStatus = new JLabel();
	
	//Indicate selected row and column by the current move
	private int rowSelected;
	private int columnSelected;
	
	//Input and output streams from/to server
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	
	//Continue to play?
	private boolean continueToPlay = true;
	
	//Wait for the player to mark a chessman
	private boolean waiting= true;
	
	//Indicate if it runs as application
	private boolean isStandAlone = false;
	
	//Host name or ip
	private String host = "localhost";
	
	/**Initialize UI**/
	public void init(){
		//Panel p to hold cells
		Panel p = new Panel();
	    // Set properties for labels and borders for labels and panel
			p.setLayout(null);
		    jlblTitle.setHorizontalAlignment(JLabel.CENTER);
		    jlblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
		    jlblTitle.setBorder(new LineBorder(Color.black, 1));
		    jlblStatus.setBorder(new LineBorder(Color.black, 1));

		    // Place the panel and the labels to the applet
		    add(jlblTitle, BorderLayout.NORTH);
		    add(p, BorderLayout.CENTER);
		    for (int i = 0; i < NUMBER; i++)//Put all the chessmen to the chessboard 
			      for (int j = 0; j < NUMBER; j++){
			    	  chessman[i][j] = new Chessman(i, j);
			        chessman[i][j].setBounds(40+i*span_x,40+j*span_y,80,80);
			        p.add(chessman[i][j]);
			        };
		    add(jlblStatus, BorderLayout.SOUTH);
		   
		    //Set the size of the window
		    int wWidth = 880;
		    int wHeight = 880;
		    setSize(new Dimension(920,920));
		   
		//Connect to the server
		connectToServer();
	}
	
	private void connectToServer(){
		try {
			//Create a socket to connect to the server
			Socket socket;
			if (isStandAlone) 
				socket =  new Socket(host,port);
			else socket  = new Socket(getCodeBase().getHost(),port);
			
			//Create an input stream to receive data from the server
			fromServer = new DataInputStream(socket.getInputStream());
			
			//Create an output stream to send data to the server
			toServer = new DataOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			System.out.println("connectToServer Error:" + e.getMessage());
		}
		
		//Control the game on a separate thread
		Thread thread = new Thread(this);
		thread.start();
	}
	public void run() {
		try {
			//Get the notification from the server
			int player = fromServer.readInt();
			
			//Am I player 1 or 2?
			if (player == PlAYER1) {
				myToken ='B';
				otherToken = 'W';
				jlblTitle.setText("玩家1 : 执【黑】棋");
				jlblStatus.setText("等待 玩家2 加入。。。");
				
				//Receive startup notification from the server
				fromServer.readInt();//Whatever read is ignored
				
				//The other player has joined
				jlblStatus.setText("玩家2 已经加入。 我先开始！");
				
				//It is my turn
				myTurn =true;
			}
			else if (player == PlAYER2){
				myToken = 'W';
				otherToken = 'B';
				jlblTitle.setText("玩家2: 执【白】棋");
				jlblStatus.setText("等待 玩家1 下一步。。。");
			}
			
			//Continue to play
			while(continueToPlay){
				if (player == PlAYER1) {
					waitForPlayerAction();
					SendMove();
					receiveInfoFromServer();
				}
				else if (player == PlAYER2) {
					receiveInfoFromServer();
					waitForPlayerAction();
					SendMove();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	/**Wait for the player to mark a chessman**/
	private void waitForPlayerAction()throws InterruptedException {
		while(waiting){
			Thread.sleep(100);}
		
		waiting = true;
	}
	
	/**Send this player's move to the server**/
	private void SendMove() throws IOException {
		toServer.writeInt(rowSelected);
		toServer.writeInt(columnSelected);
	}
	
	/**Receive info from the server*/
	private void receiveInfoFromServer() throws IOException{
		//Receive game status
		int status = fromServer.readInt();
		if (status == PlAYER1_WON) {
			//Player 1 won, stop playing
			continueToPlay = false;
			if (myToken=='B') 
				jlblStatus.setText("我赢了。【黑棋】胜");
			else if (myToken =='W'){
				jlblStatus.setText("玩家1 【黑棋】 赢了！");
				receiveMove();
			}
			
		}
		else if (status==PlAYER2_WON){
			//Player 2 won,stop playing
			continueToPlay = false;
			if (myToken=='B') {
				jlblStatus.setText("玩家2 【白棋】 赢了！");
				receiveMove();
			}
			else if (myToken =='W')
				jlblStatus.setText("我赢了。【白棋】胜！");
		}
		else if(status == DRAW){
			//No winner,game is over
			continueToPlay = false;
			jlblStatus.setText("游戏结束。没有赢家。");
			
			if (myToken == 'W') {
				receiveMove();
			}
		}
		else {
			receiveMove();
			jlblStatus.setText("轮到我。");
			myTurn = true;//It is my turn 
		}
	}
	private void receiveMove() throws IOException {
		//Get the other player's move
		int row = fromServer.readInt();
		int column =  fromServer.readInt();
		chessman[row][column].setToken(otherToken);
	}
	
	//An inner class for a chessman
	public class Chessman extends JPanel{
		//Indicate the row and column of this chessman in the board
		private int row;
		private int column;
		
		//Token used for this chessman
		private char token =' ';
		
		public Chessman(int row,int column){
			this.column = column;
			this.row = row;
			addMouseListener(new ClickListener());//Register listener
		}
		
		/** Return token */
		public char getToken(){
			return token;
		}
		public void setToken(char c){
			token =c;
			repaint();
		}
		
		/** Paint the chessman */
		 protected void paintComponent(Graphics g) {
		      super.paintComponent(g);
		      if (token != ' ') {
		    // Am I the Black one or the White one?
		      if (token == 'B') {
		         g.setColor(Color.BLACK);//Set the black color
		      }
		      else if (token == 'W') {
		        g.setColor(Color.WHITE);
		      }
		      g.fillOval(10, 10, getWidth() - 20, getHeight() - 20);//Draw a chessman
		      }
		    }
		
		/** Handle mouse click on a chessman */
		private class ClickListener extends MouseAdapter{
			public void mouseClicked(MouseEvent e)
			{
				//if chessman is not occupied and the player has the turn 
				if((token == ' ')&& myTurn){
					setToken(myToken);//Set the player has the turn
					myTurn = false;
					rowSelected =  row;
					columnSelected = column;
					jlblStatus.setText("等待另一玩家下棋！");
					waiting = false;//Just completed a successful move
				}
				
			}
		}
	}
	/**Extends Panel class to draw the chessboard*/
	class Panel extends JPanel
	{
		Image img = Toolkit.getDefaultToolkit().getImage("images/chessboard800800.jpg");
	public void paint(Graphics g){
	super.paint(g);
	 int i=1;
     //画横线  
     for(;i<=NUMBER;i++)  
         g.drawLine(x+40, y+i*span_y-40, FWidth-x-40,y+i*span_y-40);
     
     //画竖线  
     i = 0;
     for(;i<=NUMBER;i++)  
     {  
         g.drawLine(x+i*span_x-40, y+40, x+i*span_x-40,FHeight-y-40);  
     }  
     Graphics2D g2 = (Graphics2D)g;  //g是Graphics对象  
     g2.setStroke(new BasicStroke(3.0f)); 
     g2.drawLine(x, y, FWidth-x,y);//North
     g2.drawLine(x, y, x,FHeight-y); //West 
     g2.drawLine(x, y+(i-1)*span_y, FWidth-x,y+(i-1)*span_y);//South
     g2.drawLine(x+(i-1)*span_x, y, x+(i-1)*span_x,FHeight-y);//East
     
	 repaint();
	}
	}
	


/** This main method enables the applet to run as an application */
public static void main(String[] args) {
   // Create a frame
   JFrame frame = new JFrame("Five-in-a-row Client");

   // Create an instance of the applet
   FIRClient applet = new FIRClient();
   applet.isStandAlone = true;

   // Get host
   if (args.length == 1) applet.host = args[0];

   // Add the applet instance to the frame
   frame.getContentPane().add(applet, BorderLayout.CENTER);

   // Invoke init() and start()
   applet.init();
   applet.start();

   // Display the frame
   frame.setSize(980,980);
   frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   frame.setVisible(true);
 }
}
