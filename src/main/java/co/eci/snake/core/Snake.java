package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Snake {
  private final Deque<Position> body = new ArrayDeque<>();
  private volatile Direction direction;
  private int maxLength = 5;

  private volatile boolean dead = false;
  private volatile long deathTime = Long.MAX_VALUE;
  private final int id;
  private static int nextId = 0;

  private volatile int hits = 0;

  private Snake(Position start, Direction dir) {
    body.addFirst(start);
    this.direction = dir;
    this.id = nextId++;
  }

  public static Snake of(int x, int y, Direction dir) {
    return new Snake(new Position(x, y), dir);
  }

  public synchronized Direction direction() { return direction; }

  public synchronized  void turn(Direction dir) {
    if ((direction == Direction.UP && dir == Direction.DOWN) ||
        (direction == Direction.DOWN && dir == Direction.UP) ||
        (direction == Direction.LEFT && dir == Direction.RIGHT) ||
        (direction == Direction.RIGHT && dir == Direction.LEFT)) {
      return;
    }
    this.direction = dir;
  }

  public synchronized Position head() { return body.peekFirst(); }

  public synchronized Deque<Position> snapshot() { return new ArrayDeque<>(body); }

  public synchronized void advance(Position newHead, boolean grow) {
    body.addFirst(newHead);
    if (grow) maxLength++;
    while (body.size() > maxLength) body.removeLast();
  }

  public synchronized int length() { return body.size(); }

  public void markDead() {
    if (!dead) {
      dead = true;
      deathTime = System.currentTimeMillis();
    }
  }

  public boolean isDead() { return dead; }

  public long getDeathTime() { return deathTime; }

  public int getId() { return id; }

  public synchronized void addHit() {
    hits++;
  }
  public int getHits() {
    return hits;
  }
}
