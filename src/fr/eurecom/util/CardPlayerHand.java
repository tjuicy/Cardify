package fr.eurecom.util;

import java.util.LinkedList;
import java.util.List;

import android.graphics.Point;
import fr.eurecom.cardify.Game;

public class CardPlayerHand {

	public List<Card> cardStack;
	public List<Card> cardHeap;
	public Game game;
	
	public CardPlayerHand(Game game){
		this.game = game;
		cardStack = new LinkedList<Card>();
		cardHeap = new LinkedList<Card>();
	}
	
	public void dealInitialCards(List<Card> cards){
		this.cardStack = cards;
		stackCards();
		for (Card c : cardStack){
			game.addView(c);
			c.setOwner(this);
		}
	}
	
	public void addToStack(Card card){
		cardStack.remove(card);
		cardHeap.remove(card);
		int pos = 0;
		for (Card c : cardStack){
			if (c.getX() >= card.getX()) break;
			pos++;
		}
		cardStack.add(pos, card);
		stackCards();
	}
	
	public void addToHeap(Card card){
		cardStack.remove(card);
		cardHeap.remove(card);
		cardHeap.add(card);
		stackCards();
	}
	
	public void stackCards(){
		if (cardStack.isEmpty()) return;
		Point displaySize = game.getDisplaySize();
		int pixelsBetweenCards = displaySize.x/cardStack.size() - (Card.width-displaySize.x/cardStack.size())/cardStack.size();
		int y = displaySize.y-Card.height;
		int x = 0;
		
		for (int i = 0; i < cardStack.size(); i++) {
			Card card = cardStack.get(i);
			x = i*pixelsBetweenCards;
			card.setX(x);
			card.setY(y);
		}
	}
	
	public boolean inStackZone(float x, float y){
		Point displaySize = game.getDisplaySize();
		if (x < 0 || x > displaySize.x) return false;
		if (y < displaySize.y - 2*Card.height || y > displaySize.y) return false;
		return true;
	}
	
	public void moveCard(Card card){
		if (inStackZone(card.getX(), card.getY())){
			addToStack(card);
		} else {
			addToHeap(card);
		}
	}
	
}
