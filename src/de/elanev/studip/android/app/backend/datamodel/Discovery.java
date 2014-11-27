package de.elanev.studip.android.app.backend.datamodel;

import java.util.HashMap;
import java.util.Map;

public class Discovery {
	public Map<String, Route> routes;

	public Discovery() {
		routes = new HashMap<String, Route>();
	}
}
