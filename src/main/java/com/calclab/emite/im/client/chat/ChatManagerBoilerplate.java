/*
 * ((e)) emite: A pure Google Web Toolkit XMPP library
 * Copyright (c) 2008-2011 The Emite development team
 * 
 * This file is part of Emite.
 *
 * Emite is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Emite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Emite.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.calclab.emite.im.client.chat;

import java.util.Collection;
import java.util.HashSet;

import com.calclab.emite.core.client.events.ChangedEvent.ChangeType;
import com.calclab.emite.core.client.events.MessageReceivedEvent;
import com.calclab.emite.core.client.xmpp.session.SessionState;
import com.calclab.emite.core.client.xmpp.session.SessionStateChangedEvent;
import com.calclab.emite.core.client.xmpp.session.XmppSession;
import com.calclab.emite.core.client.xmpp.stanzas.Message;
import com.calclab.emite.core.client.xmpp.stanzas.XmppURI;
import com.google.web.bindery.event.shared.EventBus;

public abstract class ChatManagerBoilerplate<C extends Chat> implements ChatManager<C>, SessionStateChangedEvent.Handler, MessageReceivedEvent.Handler {
	
	protected final EventBus eventBus;
	protected final XmppSession session;
	protected ChatSelectionStrategy strategy;
	
	protected final HashSet<C> chats;
	private XmppURI currentChatUser;

	public ChatManagerBoilerplate(final EventBus eventBus, final XmppSession session, final ChatSelectionStrategy strategy) {
		this.eventBus = eventBus;
		this.session = session;
		this.strategy = strategy;
		
		chats = new HashSet<C>();
		
		session.addMessageReceivedHandler(this);
		
		// Control chat state when the user logout and login again
		session.addSessionStateChangedHandler(true, this);
	}
	
	@Override
	public void onMessageReceived(final MessageReceivedEvent event) {
		final Message message = event.getMessage();
		final ChatProperties properties = strategy.extractProperties(message);
		if (properties != null) {
			C chat = getChat(properties, false);
			if (chat == null && properties.shouldCreateNewChat()) {
				// we need to create a chat for this incoming message
				properties.setInitiatorUri(properties.getUri());
				chat = addNewChat(properties);
			}
			if (chat != null) {
				chat.receive(message);
			}
		}
	}
	
	@Override
	public void onSessionStateChanged(final SessionStateChangedEvent event) {
		if (event.is(SessionState.loggedIn)) {
			final XmppURI currentUser = session.getCurrentUserURI();
			if (currentChatUser == null) {
				currentChatUser = currentUser;
			}
			if (currentUser.equalsNoResource(currentChatUser)) {
				for (final C chat : chats) {
					chat.open();
				}
			}
		} else if (event.is(SessionState.loggingOut) || event.is(SessionState.disconnected)) {
			// check both states: loggingOut is preferred, but not
			// always fired (i.e. error)
			for (final C chat : chats) {
				chat.close();
			}
		}
	}
	
	protected abstract void fireChanged(ChangeType type, C chat);
	
	@Override
	public C getChat(final ChatProperties properties, final boolean createIfNotFound) {
		for (final C chat : chats) {
			if (strategy.isAssignable(chat.getProperties(), properties))
				return chat;
		}
		if (createIfNotFound) {
		}
		return null;
	}

	@Override
	public C getChat(final XmppURI uri) {
		return getChat(new ChatProperties(uri), false);
	}

	@Override
	public Collection<C> getChats() {
		return chats;
	}

	@Override
	public C open(final XmppURI uri) {
		return openChat(new ChatProperties(uri), true);
	}

	@Override
	public C openChat(final ChatProperties properties, final boolean createIfNotFound) {
		C chat = getChat(properties, false);
		if (chat == null) {
			if (!createIfNotFound)
				return null;
			properties.setInitiatorUri(session.getCurrentUserURI());
			chat = addNewChat(properties);
		}
		chat.open();
		fireChanged(ChangeType.opened, chat);
		return chat;
	}
	

	@Override
	public void close(final C chat) {
		chat.close();
		getChats().remove(chat);
		fireChanged(ChangeType.closed, chat);
	}

	protected void addChat(final C chat) {
		chats.add(chat);
	}

	/**
	 * This method creates a new chat, add it to the pool and fire the event
	 * 
	 * @param properties
	 */
	protected C addNewChat(final ChatProperties properties) {
		final C chat = createChat(properties);
		addChat(chat);
		fireChanged(ChangeType.created, chat);
		return chat;
	}

	/**
	 * A template method: the subclass must return a new object of class Chat
	 * 
	 * @param properties
	 *            the properties of the chat
	 * @return a new chat. must not be null
	 */
	protected abstract C createChat(ChatProperties properties);

	@Override
	public void setChatSelectionStrategy(final ChatSelectionStrategy strategy) {
		assert strategy != null : "The ChatSelectionStrategy can't be null!";
		this.strategy = strategy;
	}
	
}
