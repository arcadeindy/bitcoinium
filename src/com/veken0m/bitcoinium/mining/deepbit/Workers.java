package com.veken0m.bitcoinium.mining.deepbit;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Workers {
	private List<Worker> workers = new ArrayList<Worker>();
	private List<String> names = new ArrayList<String>();

	public Worker getWorker(int i) {
		return workers.get(i);
	}

	public List<String> getNames() {
		return names;
	}

	public List<Worker> getWorkers() {
		return workers;
	}

	public int numberOfWorkers() {
		return names.size();
	}

	@JsonAnySetter
	public void setWorker(String name, Worker worker) {
		this.workers.add(worker);
		this.names.add(name);
	}

}