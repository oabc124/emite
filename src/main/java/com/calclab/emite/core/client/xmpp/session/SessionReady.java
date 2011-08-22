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

package com.calclab.emite.core.client.xmpp.session;

import java.util.logging.Logger;

import com.calclab.emite.core.client.xmpp.stanzas.Presence;
import com.google.inject.Inject;

/**
 * A simple component that sets the session ready after logged in. This
 * component is removed if InstantMessagingModule used.
 */
public class SessionReady implements SessionStateChangedEvent.Handler {

	private static final Logger logger = Logger.getLogger(SessionReady.class.getName());
	
	private final XmppSession session;
	
	private boolean enabled = true;

	@Inject
	public SessionReady(final XmppSession session) {
		this.session = session;

		session.addSessionStateChangedHandler(true, this);
	}
	
	@Override
	public void onSessionStateChanged(final SessionStateChangedEvent event) {
		if (enabled) {
			if (event.is(SessionState.loggedIn)) {
				session.send(new Presence());
				session.setSessionState(SessionState.ready);
			}
		}
	}

	public void setEnabled(final boolean enabled) {
		logger.finer("SessionReady - enabled: " + enabled);
		this.enabled = enabled;
	}

}
