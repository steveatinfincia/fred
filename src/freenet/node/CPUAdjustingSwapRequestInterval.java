package freenet.node;

import freenet.support.Logger;

/**
 * @author amphibian
 * 
 * Start at a given default value, adjust up or down according to
 * CPU usage for a given target % usage.
 */
public class CPUAdjustingSwapRequestInterval implements SwapRequestInterval, Runnable {

    double currentValue;
    int targetCPUUsage;
    CPUUsageMonitor m;
    final double mulPerSecond = 1.05;
    final double max = Double.MAX_VALUE / 1.05;
    final double min = Double.MIN_VALUE;
    
    CPUAdjustingSwapRequestInterval(double initialValue, int targetCPUUsage) {
        currentValue = initialValue;
        this.targetCPUUsage = targetCPUUsage;
        m = new CPUUsageMonitor();
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    public synchronized double getValue() {
        return currentValue;
    }

    public void run() {
        while(true) {
            long now = System.currentTimeMillis();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            int cpuUsage = m.getCPUUsage();
            long endSleepTime = System.currentTimeMillis();
            double mul = Math.pow(mulPerSecond, ((double)(endSleepTime-now))/1000);
            if(cpuUsage == -1) {
                Logger.error(this, "Cannot auto-adjust based on CPU usage");
                return;
            }
            synchronized(this) {
                if(cpuUsage > targetCPUUsage) {
                    if(currentValue < max)
                        currentValue *= mul; // 5% slower per second
                } else if(cpuUsage < targetCPUUsage) {
                    if(currentValue > min)
                        currentValue /= mul; // 5% faster per second
                }
                if(currentValue < min) currentValue = min;
                if(currentValue > max) currentValue = max;
            }
            Logger.minor(this, "CPU usage: "+cpuUsage+" target "+targetCPUUsage+" current value: "+currentValue);
        }
    }
}
