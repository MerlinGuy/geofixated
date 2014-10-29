package org.geof.test;

public class TestRunnable implements Runnable {

	private int count = 0;
	private boolean _is_running = true;
	@Override
	public void run() {
		while (count < 5) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Count : "  + count);
			count++;
		}
		_is_running = false;
	}
	
	public boolean isRunning(){
		return _is_running;
	}

}
