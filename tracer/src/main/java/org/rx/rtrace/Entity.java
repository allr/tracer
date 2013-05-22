package org.rx.rtrace;

public abstract class Entity<T> {
	protected T id;

	public Entity() {
		this(null);
	}
	
	public Entity(T id) {
		this.id = id;
	}

	public T getId() {
		return id;
	}

	public void setId(T id) {
		this.id = id;
	}
	
	public void clear() {
		this.id = null;
	}
	
	public boolean isNew() {
		return id == null;
	}

}
