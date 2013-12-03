package fr.eurecom.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import fr.eurecom.cardify.Game;

public class CardPlayerHand {

	public List<Card> cardStack;
	public List<Card> cardPublic;
	public Game game;
	private Point displaySize;
	
	public CardPlayerHand(Game game){
		this.game = game;
		cardStack = new LinkedList<Card>();
		cardPublic = new LinkedList<Card>();
		displaySize = game.getDisplaySize();
	}
	
	public void dealInitialCards(List<Card> cards){
		cardStack = cards;
		Collections.sort(cardStack, new CardComparator(CardSortingRule.S_H_D_C_ACE_HIGH));
		stackCards();
		for (Card c : cardStack){
			game.addView(c);
			c.setOwner(this);
		}
	}
	
	public void addToStack(Card card){
		cardStack.remove(card);
		int pos = 0;
		for (Card c : cardStack){
			if (c.getX() >= card.getX()) break;
			pos++;
		}
		cardStack.add(pos, card);
		stackCards();
	}
	
	public void addToPublic(Card card){
		if (cardStack.remove(card)){
			cardPublic.add(card);
			game.getClient().publishPutCardInPublicZone(card);
		}
		stackCards();
	}
	
	public void removeFromPublic(Card card) {
		if (cardPublic.remove(card)) {
			cardStack.add(card);
			game.getClient().publishTakeCardFromPublicZone(card);
		}
	}
	
	public void blindAddToPublic(char suit, int face){
		Log.e("CardPlayerHand:blindAddToPublic", ""+suit+face);
		Card card = new Card(this.game, suit, face);
		card.setOwner(this);
		cardPublic.add(card);
		animateCardIntoView(card);
		
		game.printMessage("?? played "+suit+face);
	}
	
	public void blindRemoveFromPublic(char suit, int face){
		Card cardToRemove = null;
		for (Card card : cardPublic){
			if (card.getSuit() == suit && card.getFace() == face){
				cardToRemove = card;
				break;
			}
		}
		if (cardPublic.remove(cardToRemove)){
			game.removeView(cardToRemove);
		}
		
		game.printMessage("?? took "+suit+face+" from the table");
	}
	
	private void animateCardIntoView(Card card) {
		card.setX(displaySize.x/2 - Card.width/2);
		card.setY(0 - Card.height);
		
		int yTranslation = (displaySize.y/2 - Card.height/2);
		
		game.addView(card);
		card.animate().translationY(yTranslation).setDuration(1000).setInterpolator(new AccelerateDecelerateInterpolator());
	}
	
	public void stackCards(){
		if (cardStack.isEmpty()) return;
		
		int maximumStackWidth = displaySize.x - 10; //5px free on each side of stack
		double pixelsBetweenCards = cardStack.size() != 1 ? Math.min(Card.width/2, (maximumStackWidth - Card.width)/(cardStack.size() - 1)) : 0;
		int y = displaySize.y - Card.height;
		int x = 0;
		
		if (pixelsBetweenCards <= Card.width/2) {
			x = (int) Math.round(((displaySize.x - ((cardStack.size() - 1)*pixelsBetweenCards + Card.width))/2));
		} else {
			x = 5;
		}
		
		for (Card card : cardStack) {
			card.setX(x);
			card.setY(y);
			x += Math.round(pixelsBetweenCards);
			queueBringToFront(card);
		}
		applyBringToFront(cardStack.get(0));
	}
	
	public boolean inStackZone(float x, float y){
		if (x < 0 || x > displaySize.x) return false;
		if (y < displaySize.y - 1.75*Card.height || y > displaySize.y) return false;
		return true;
	}
	
	public void takeCard(Card card) {
		
	}
	
	public void dropCard(Card card){
		if(inStackZone(card.getX(), card.getY())) {
			removeFromPublic(card);
			addToStack(card);
		} else {
			addToPublic(card);
		}
	}
	
	public void moveCard(Card card) {
		
	}
	
	private void queueBringToFront(View v){
		v.bringToFront();
	}
	
	private void applyBringToFront(View view){
		view.requestLayout();
		view.invalidate();
	}
	
	public void sortCards(CardSortingRule sorting) {
		Collections.sort(cardStack, new CardComparator(sorting));
		stackCards();
	}
	
}
