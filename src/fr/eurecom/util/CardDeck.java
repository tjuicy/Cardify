package fr.eurecom.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import fr.eurecom.cardify.Game;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class CardDeck extends ImageView implements OnTouchListener {
	private static char[] suits = {'s', 'h', 'd', 'c'};
	private static int[] faces = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
	
	private List<Card> cards;
	private Point anchorPoint = new Point();
	private CardPlayerHand playerHand;
	private final ColorFilter highlightFilter = new LightingColorFilter(Color.DKGRAY, 1);
	private long lastDown;
	
	public CardDeck(Context context){
		super(context);
		
		this.cards = new LinkedList<Card>();
	
		for (char suit : suits){
			for (int face : faces){
				cards.add(new Card(suit, face, false));
			}
		}
		
		this.setOnTouchListener(this);
		this.setImage(context);
		
		this.setX(Game.screenSize.x/4);
		this.setY(Game.screenSize.y/4);
		this.toggleEmpty();
	}
	
	public CardDeck(Context context, List<Card> receivedCards) {
		super(context);
		
		this.cards = receivedCards;
		
		this.setOnTouchListener(this);
		this.setImage(context);
		
		this.setX(Game.screenSize.x/4);
		this.setY(Game.screenSize.y/4);
		this.toggleEmpty();
	}
	
	private void setImage(Context context) {
		this.setImageResource(context.getResources().getIdentifier("drawable/deck", null, context.getPackageName()));
	}
	
	public void setOwner(CardPlayerHand owner) {
		this.playerHand = owner;
	}
	
	public Card pop(){
		if (cards.isEmpty()) return null;
		return cards.remove(0);
	}

	public Card peak(){
		if (cards.isEmpty()) return null;
		return cards.get(0);
	}
	
	public void addCard(Card card) {
		this.cards.add(0, card);
		if (cards.size() == 1) {
			toggleEmpty();
		}
	}
	
	public List<Card> draw(int n){
		List<Card> temp = new LinkedList<Card>();
		for (int i = 0; i < n; i++){
			if (cards.isEmpty()) break;
			temp.add(this.pop());
		}
		return temp;
	}
	
	public void shuffle(){
		Collections.shuffle(this.cards);
	}
	
	public Card drawFromDeck() {
		Card c = pop();
		//TODO: Set position elsewhere
		//TODO: When no cards in stack, not stacking
		//c.setX(this.getX() + this.getWidth());
		//c.setY(this.getY());
		c.setTurned(true);
		
		if (cards.isEmpty()) {
			toggleEmpty();
		}
		
		return c;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final int action = event.getAction();
		
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if(lastDown != 0 && (System.currentTimeMillis() - lastDown) <= 200 && !cards.isEmpty()) {
				playerHand.drawFromDeck(drawFromDeck());
				return true;
			}
			lastDown = System.currentTimeMillis();
			
			anchorPoint.x = (int) (event.getRawX() - v.getX());
			anchorPoint.y = (int) (event.getRawY() - v.getY());
			
			if (!cards.isEmpty()) v.setAlpha((float)0.5);
			v.bringToFront();
			
			return true;
			
		case MotionEvent.ACTION_MOVE:
			int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            
            float posX = x-anchorPoint.x;
            float posY = y-anchorPoint.y;
            
            if(posX < 0 || (posX+getWidth()) > Game.screenSize.x) {
            	if(posY > 0 && (posY+getHeight()) < Game.screenSize.y - getHeight()) {
            		v.setY(y-(anchorPoint.y));
            	}
            } else if(posY < 0 || (posY+getHeight()) > Game.screenSize.y - getWidth()) {
            	if(posX > 0 && (posX+getWidth()) < Game.screenSize.x) {
            		v.setX(x-(anchorPoint.x));
            	}
            } else {
            	v.setX(x-(anchorPoint.x));
            	v.setY(y-(anchorPoint.y));
            }
            
            return true;
		case MotionEvent.ACTION_UP:
			if (!cards.isEmpty()) v.setAlpha(1);
			return true;
		default:
			return false;
		}
	}
	
	public void toggleHighlight(boolean highlight) {
		if (highlight) {
			this.setColorFilter(highlightFilter);
		} else {
			this.setColorFilter(null);
		}
	}
	
	public void toggleEmpty() {
		if (cards.isEmpty()) {
			this.setAlpha((float)0.3);
		} else {
			this.setAlpha((float)1.0);
		}
	}
	
	public List<Card> getCards() {
		return cards;
	}
}
