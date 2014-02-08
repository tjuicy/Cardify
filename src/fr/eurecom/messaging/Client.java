package fr.eurecom.messaging;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import fr.eurecom.cardify.Game;
import fr.eurecom.cardify.Lobby;
import fr.eurecom.util.Card;
import fr.eurecom.util.CardDeck;

public class Client implements Handler.Callback {

	private Activity activity;
	private Set<InetAddress> receivers;
	private Sender sender;
	private boolean isHost;
	private int peersInitialized;
	private MessageListener receivingThread;
	private Handler handler;
	
	public Client(Activity activity) {
		this.activity = activity;

		handler = new Handler(this);
		try {
			receivingThread = new MessageListener(handler);
			receivingThread.start();
		} catch (IOException e) {
			Log.e("Client", "Constructor error: __" + e.getMessage());
		}
		
		

		receivers = new HashSet<InetAddress>();
		this.sender = new Sender();
		this.isHost = false;
		this.peersInitialized = 0;
		
	}
	
	public void handleThreadMessage(GameMessage gameMessage, int what) {
		Log.d("HandleThreadMessage", "Mottatt melding fra en annen tråd:");
		
		Message completeMessage = handler.obtainMessage(what, gameMessage);
		completeMessage.sendToTarget();
	}
	
	public void disconnect(){

		receivingThread.stopThread();
	}
	
	
	public void changeToHost(){
		this.isHost = true;
	}
	
	public boolean isHost(){
		return this.isHost;
	}
	
	public void addReceiver(InetAddress receiver) {
		receivers.add(receiver);
		for (InetAddress r : receivers) {
			Log.e("Receiver", r.toString());
		}
	}
	
	public void registerAtHost() {
		sendMessage(Action.REGISTER, "");
	}
	
	public Set<InetAddress> getReceivers(){
		return receivers;
	}
	
	public void broadcastStartGame() {
		sendMessage(Action.GAME_STARTED, "");
	}
	
	public void publishDrawFromDeckToStack(Card card) {
		sendMessage(Action.DREW_FROM_DECK_TO_STACK, card.toString());
	}
	
	public void publishTakeCardFromPublicZone(Card card) {
		sendMessage(Action.REMOVED_CARD_FROM_PUBLIC_ZONE, card.toString());
	}

	public void publishPutCardInPublicZone(Card card, float x, float y) {
		sendMessage(Action.ADDED_CARD_TO_PUBLIC_ZONE, card.toStringWithPosition(x, y));
	}
	
	public void publishTurnCardInPublicZone(Card card) {
		sendMessage(Action.TURNED_CARD_IN_PUBLIC_ZONE, card.toString());
	}
	
	public void publishAddCardToDeck(Card card) {
		sendMessage(Action.ADDED_CARD_TO_DECK, card.toString());
	}
	
	public void publishDisconnect() {
		sendMessage(Action.DISCONNECT, "");
	}
	
	public void pushInitialCards(CardDeck deck, int n) {
		for (InetAddress receiver : receivers){
			String cards = "";
			for (int i = 0; i < n; i++){
				if (deck.peek() == null) break;
				cards += deck.pop().toString() + ";";
			}
			GameMessage message = new GameMessage(Action.INITIAL_CARDS, cards);
			Log.e("Client", "Sending cards [" + cards + "] to " + receiver);
			
			this.sender.send(message, receiver);
		} 
	}
	
	public void publishGameInitialized() {
		sendMessage(Action.GAME_INITIALIZED, "");
	}
	
	public void pushRemainingDeck(CardDeck deck) {
		StringBuilder cardStringBuilder = new StringBuilder();
		List<Card> cards = deck.getCards();
		for (int i = 0; i < cards.size(); i++) {
			cardStringBuilder.append(cards.get(i).toString());
			cardStringBuilder.append(";");
		}
		for (InetAddress receiver : receivers) {
			GameMessage message = new GameMessage(Action.REMAINING_DECK,cardStringBuilder.toString());
			this.sender.send(message, receiver);
		}
		
	}
	
	
	private void sendMessage(Action what, String about) {
		GameMessage message = new GameMessage(what, about);
		for (InetAddress receiver : receivers){
			this.sender.send(message, receiver);
		}
	}
	
	private void broadcastChange(GameMessage message){
		for (InetAddress receiver : receivers){
			if (!receiver.equals(message.getOriginatorAddr())){
				this.sender.send(message, receiver);
			}
		}
	}
	
	
	public void handleGameMessage(GameMessage message) {
		if(message.what == Action.GAME_INITIALIZED) {
			Log.e("handleGameMessage", "Game initialized" + message.getOriginatorAddr());
			handleGameInitialized(message);
		} else if (this.activity instanceof Lobby) {
			handlePreGameMessage(message);
		} else if (this.activity instanceof Game){
			handleInGameMessage(message);
		}
	}
	
	private void handlePreGameMessage(GameMessage message){
		switch (message.what){
		case GAME_STARTED:
			handleGameStarted(message);
			return;
		case REGISTER:
			handleRegister(message);
			return;
		case DISCONNECT:
			handlePreGameDisconnect(message);
			return;
		default:
			return;
		}
	}
	
	private void handleRegister(GameMessage message) {
		addReceiver(message.getOriginatorAddr());
		((Lobby) activity).dismissProgressDialog();
	}
	
	private void handleGameStarted(GameMessage message) {
		//TODO:
		/*
		 * Start new game and set local game variable in this interpreter to game instance
		 */
		((Lobby) activity).startGameActivity(receivers);
	}
	
	private void handleGameInitialized(GameMessage message) {
		if (isHost) {
			peersInitialized++;
			Log.e("HORE", "Number of peers initialized: " + peersInitialized);
			if (peersInitialized == receivers.size()) {
				((Lobby) activity).startGameActivity(receivers, true);				
			}
		} 
	}
	
	public void broadcastGameInitialized() {
		sendMessage(Action.GAME_INITIALIZED, "");
	}
	
	private void handleInGameMessage(GameMessage message){
		switch (message.what) {
		case ADDED_CARD_TO_PUBLIC_ZONE:
			handleAddedCardToPublicZone(message);
			return;
		case REMOVED_CARD_FROM_PUBLIC_ZONE:
			handleRemovedCardFromPublicZone(message);
			return;
		case TURNED_CARD_IN_PUBLIC_ZONE:
			handleTurnedCardInPublicZone(message);
			return;
		case ADDED_CARD_TO_DECK:
			handleAddCardToDeck(message);
			return;
		case DREW_FROM_DECK_TO_STACK:
			handleDrewFromDeckToStack(message);
			return;
		case ILLEGAL_ACTION:
			return;
		case INITIAL_CARDS:
			handleInitialCards(message);
			return;
		case REMAINING_DECK:
			handleRemainingDeck(message);
			return;
		case DISCONNECT:
			handleInGameDisconnect(message);
			return;
		default:
			return;
		}
	}
	
	private void handleInGameDisconnect(GameMessage message) {
		if (isHost) {
			//TODO: What should the host do when someone disconnects from the game?
			Log.e("Client", "Should remove " + message.getOriginatorAddr().toString() + " from stack");
			Log.e("Client", "Stack contains ? " + receivers.contains(message.getOriginatorAddr()));
			receivers.remove(message.getOriginatorAddr());
			if (receivers.size() == 0) {
				((Game) activity).exitGame();
			}
		} else {
			((Game) activity).exitGame();
		}
	}
	
	//TODO: Dette må fikses!
	private void handlePreGameDisconnect(GameMessage message) {
		if (isHost) {
			Log.e("Client", "Should remove " + message.getOriginatorAddr().toString() + " from stack");
			Log.e("Client", "Stack contains ? " + receivers.contains(message.getOriginatorAddr()));
			receivers.remove(message.getOriginatorAddr());
			if (receivers.size() > 0) {
				Log.e("Client", "Disconnect. Receivers > 0");
				return;
			} 
		}
		Log.e("Client", "Other device disconnected");
		((Lobby) activity).resetLobby();
	}
	

	private void handleDrewFromDeckToStack(GameMessage message) {
		Log.e("Client:handleDrewFromDeckToStack", "RUN: " + message.about);
		((Game) activity).getPlayerHand().blindDrawFromDeckToStack();
		if (isHost) {
			broadcastChange(message);
		}
	}
	

	private void handleAddedCardToPublicZone(GameMessage message) {

		Log.e("Client:handleAddedCardToPublicZone", "RUN: " + message.about);
		
		// 0s10@0.231,0.423 --> 0s10 and 0.231,0.423
		String[] token = message.about.split("@");
		
		boolean turned = token[0].charAt(0) == '1' ? true : false;
		char suit = token[0].charAt(1);
		int face = Integer.parseInt(token[0].substring(2));
		
		String[] location = token[1].split(",");
		float x = Float.parseFloat(location[0]);
		float y = Float.parseFloat(location[1]);
		
		((Game) activity).getPlayerHand().blindAddToPublic(suit, face, turned, x, y);
		if (isHost) { 
			broadcastChange(message);
		}
	}
	
	private void handleRemovedCardFromPublicZone(GameMessage message) {
		Log.e("Client:handleAddedCardToPublicZone", "RUN: " + message.about);
		char suit = message.about.charAt(1);
		int face = Integer.parseInt(message.about.substring(2));
		((Game) activity).getPlayerHand().blindRemoveFromPublic(suit, face);
		if (isHost) {
			broadcastChange(message);
		}
	}
	
	private void handleTurnedCardInPublicZone(GameMessage message) {
		Log.e("Client:handleAddedCardToPublicZone", "RUN: " + message.about);
		char suit = message.about.charAt(1);
		int face = Integer.parseInt(message.about.substring(2));
		((Game) activity).getPlayerHand().blindTurnInPublic(suit, face);
		if (isHost) {
			broadcastChange(message);
		}
	}
	
	private void handleInitialCards(GameMessage message) {
		Log.e("Client:handleInitialCards", "Cards: " + message.about + " length: " + message.about.length());
		if (message.about.length() > 0) {
			String[] cards = message.about.split(";");
			((Game) activity).getPlayerHand().blindDealCards(cards);
		}
	}
	
	private void handleRemainingDeck(GameMessage message) {
		Log.e("Client:handleRemainingDeck", "Cards: " +message.about);
		String[] cards = message.about.length() == 0 ? null : message.about.split(";");
		((Game) activity).getPlayerHand().blindAddDeck(cards);
		((Game) activity).dismissProgressDialog();
	}
	
	private void handleAddCardToDeck(GameMessage message) {
		Log.e("Client:handleAddCardToDeck", "Cards: " + message.about);
		char suit = message.about.charAt(1);
		int face = Integer.parseInt(message.about.substring(2));
		boolean turned = message.about.charAt(0) == '1' ? true : false;
		((Game) activity).getPlayerHand().blindAddToDeck(suit, face, turned);
		if (isHost) { 
			broadcastChange(message);
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		//TODO: Her gjøres handleGameMessage. What må gjøres om
		if (msg.what == Config.GAME_MESSAGE_INT) {
			Log.d("Client", "Got message ");
			GameMessage gameMessage = (GameMessage) msg.obj;
			Log.d("Client", "Message is " + gameMessage.about);
			handleGameMessage(gameMessage);
		} 
		return true;
	}

}
