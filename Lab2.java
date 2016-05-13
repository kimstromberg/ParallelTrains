import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import TSim.*;

/*
 * Don't remember how to write proper JavaDoc comment header thingy
 * nor where to place it.
 * @Kim Strömberg
 * @Daniel Persson
 * Written for Parallel Programming course TDAxx2?
 * Such Parallel
 * Much Complex
 * Very Train
*/
public class Lab1{
    private TSimInterface sim = TSimInterface.getInstance();
    private int train1Speed = 10;
    private int train2Speed = 10;
    private int simSpeed = 100;

    //TopStation = b,c,e (b1)
    TrainMonitor tm0 = new TrainMonitor();

    // * Crossing = a,b,c,d
    TrainMonitor tm1 = new TrainMonitor();

    //TopRight = g,h
    TrainMonitor tm2 = new TrainMonitor();

    //Middle = k,j
    TrainMonitor tm3 = new TrainMonitor();

    //BottomLeft = m,n
    TrainMonitor tm4 = new TrainMonitor();

    //BottomStation = p (b3)
    TrainMonitor tm5 = new TrainMonitor();

    public Lab2(String[] args) {
    //Initialization of all variables

        switch(args.length){
            case 1:
                train1Speed = Integer.parseInt(args[0]);
                break;
            case 2:
                train1Speed = Integer.parseInt(args[0]);
                train2Speed = Integer.parseInt(args[1]);
                break;
            case 3:
                train1Speed = Integer.parseInt(args[0]);
                train2Speed = Integer.parseInt(args[1]);
                simSpeed = Integer.parseInt(args[2]);
                break;
            default:
                System.err.println("Error, too many parameters");
                System.exit(1);
                break;
        }
        tm0.enter();
        tm5.enter();
        (new Thread(new Train(1,train1Speed,true))).start();
        (new Thread(new Train(2,train2Speed,false))).start();
    }

    public void trainStop(Train train){
        try{
            sim.setSpeed(train.id,0);
            train.goingDown=!train.goingDown;
            Thread.sleep(1000+2*simSpeed*Math.abs(simSpeed));
            train.speed = -train.speed;
            sim.setSpeed(train.id, train.speed);
        }catch(CommandException err){
            System.err.println(err.getMessage());
            System.exit(1);
        }catch(InterruptedException err){
            System.err.println(err.getMessage());
            System.exit(1);
        }
    }

    public void trainRunning(Train train, SensorEvent e){
/******************************************Crossing. Sensors a-b-c-d*************************************************/
        //Sensor a(9,5) aquires sempahore Crossing[1]
        try{
            if(e.getXpos()==9 && e.getYpos()==5 && e.getStatus()==e.ACTIVE){
                if(train.goingDown){
                    sim.setSpeed(e.getTrainId(),0);
                    tm1.enter();
                    sim.setSpeed(e.getTrainId(),train.speed);
                }else{
                    tm1.exit(); // releases the crossing sempahore if going up
                }
            }
            //Sensor b(7,3) aquires sempahore Crossing[1] or release Crossing[1]
            if(e.getXpos()==7 && e.getYpos()==3 && e.getStatus()==e.ACTIVE){
                if(train.goingDown){
                    sim.setSpeed(e.getTrainId(),0);
                    tm1.enter();
                    sim.setSpeed(e.getTrainId(),train.speed);
                }else{
                    tm1.exit(); // releases the crossing sempahore if going up
                }
            }
            //Sensor c(12,7) Sensor on Semaphore s0 TopStation
            if(e.getXpos()==12 && e.getYpos()==7 && e.getStatus()==e.ACTIVE){
                if(train.goingDown){
                    tm1.exit(); // releases the crossing sempahore if going down
                    sim.setSpeed(e.getTrainId(),0);
                    tm2.enter(); //checks if semaphore TopRight[2] is available and takes it
                    sim.setSwitch(17,7,sim.SWITCH_RIGHT); // Changes the switch settings EFG
                    sim.setSpeed(e.getTrainId(),train.speed);
                }else{
                    tm2.exit(); // releases TopRight Semaphore
                    do
                        sim.setSpeed(e.getTrainId(),0);
                    while(!tm1.tryFree()); //tries to aquire the crossing semaphore if going up
                    sim.setSpeed(e.getTrainId(),train.speed);
                }
            }
            //Sensor d(12,8) Sensor on topStationLongPath
            if(e.getXpos()==12 && e.getYpos()==8 && e.getStatus()==e.ACTIVE){
                if(train.goingDown){
                    tm1.exit(); // releases the crossing sempahore if going down
                    sim.setSpeed(e.getTrainId(),0);
                    tm2.enter(); //checks if semaphore TopRight[2] is available
                    sim.setSwitch(17,7,sim.SWITCH_LEFT); //Sets swtich EFG
                    sim.setSpeed(e.getTrainId(),train.speed);
                }else{
                    tm2.exit();
                    sim.setSpeed(e.getTrainId(),0);
                    tm1.enter();
                    sim.setSpeed(e.getTrainId(),train.speed);
                }
            }
/*********************************************************************************************************************/


/*******************************Semaphore s2 - Sensor E****************************************************/

            //Sensor e(19,9) Sensor on semaphore s2
            if(e.getXpos()==19 && e.getYpos()==9 && e.getStatus()==e.ACTIVE){
                if(!train.goingDown){
                    if(train.bottomPath){
                        train.bottomPath = false;
                    }else{
                        tm3.exit();
                    }
                    if(tm0.tryFree()){ //looks if there is a permit available in s0
                        sim.setSwitch(17,7,sim.SWITCH_RIGHT); //sets the EFG switch to go straight
                    }else{
                        train.topStationLongPath = true;
                        sim.setSwitch(17,7,sim.SWITCH_LEFT); //TopStation occupied, sets switch to go down
                    }
                }else{
                    if(train.topStationLongPath){
                        train.topStationLongPath = false;
                    }else{
                        tm0.exit();
                    }
                    if(tm3.tryFree()){ //tries to aquire middle semaphore
                        sim.setSwitch(15,9,sim.SWITCH_RIGHT); // sets the HIJ switch to go straight
                    }else{
                        train.bottomPath = true;
                        sim.setSwitch(15,9,sim.SWITCH_LEFT); //sets the HIJ to go down, Middle semaphore busy.
                    }
                }
            }
/*********************************************************************************************************************/

/******************************************Middle Right Crossing. Sensors H-I-J***************************************/
            //Sensor F1(12,9) aquires/releases semaphore TopStation[0]/TopRight[2] and sets switch EFG
            if(e.getXpos()==12 && e.getYpos()==9 && e.getStatus()==e.ACTIVE){
                if(train.goingDown){
                    tm2.exit();
                }else{
                    sim.setSpeed(e.getTrainId(),0);
                    tm2.enter();
                    sim.setSwitch(15,9,sim.SWITCH_RIGHT); // sets the HIJ switch to go straight
                    sim.setSpeed(e.getTrainId(),train.speed);
                }
            }
            //Sensor F2(7,9) aquires/releases semaphore TopStation[0]/TopRight[2] and sets switch EFG
            if(e.getXpos()==7 && e.getYpos()==9 && e.getStatus()==e.ACTIVE){
                if(train.goingDown){
                    sim.setSpeed(e.getTrainId(),0);
                    tm4.enter();
                    sim.setSwitch(4,9,sim.SWITCH_LEFT);
                    sim.setSpeed(e.getTrainId(),train.speed);
                }else{
                    tm4.exit();
                }
            }
            //Sensor G(9,10) aquires/releases semaphore TopStation[0]/TopRight[2] and sets switch EFG
            if(e.getXpos()==9 && e.getYpos()==10 && e.getStatus()==e.ACTIVE){
                if(train.goingDown){
                    tm2.exit(); //releases TopRight[2] semaphore
                    sim.setSpeed(e.getTrainId(),0);
                    tm4.enter();
                    sim.setSwitch(4,9,sim.SWITCH_RIGHT);
                    sim.setSpeed(e.getTrainId(), train.speed);
                }else{
                    tm4.exit();
                    sim.setSpeed(e.getTrainId(),0);
                    tm2.enter();
                    sim.setSwitch(15,9,sim.SWITCH_LEFT); // sets the HIJ switch to go UP
                    sim.setSpeed(e.getTrainId(),train.speed);
                }
            }
/***************************************************************************************************************/
            //Sensor h(1,9)
            if(e.getXpos()==1 && e.getYpos()==9 && e.getStatus()==e.ACTIVE){
                if(!train.goingDown){
                    if(train.bottomStation){
                        train.bottomStation = false;
                    }else{
                        s5.release();
                    }
                    if(tm3.tryFree()){
                        sim.setSwitch(4,9,sim.SWITCH_LEFT);
                    }else{
                        train.bottomPath = true;
                        sim.setSwitch(4,9,sim.SWITCH_RIGHT);
                    }
                }else{
                    if(train.bottomPath){
                        train.bottomPath = false;
                    }else{
                        tm3.exit();
                    }
                    if(tm5.tryFree()){
                        sim.setSwitch(3,11,sim.SWITCH_LEFT);
                    }else{
                        train.bottomStation = true;
                        sim.setSwitch(3,11,sim.SWITCH_RIGHT);
                    }
                }
            }

            //Sensor p(9,11)
            if(e.getXpos()==9 && e.getYpos()==11 && e.getStatus()==e.ACTIVE){
                if(!train.goingDown){
                    sim.setSpeed(e.getTrainId(),0);
                    tm4.enter();
                    sim.setSwitch(3,11,sim.SWITCH_LEFT);
                    sim.setSpeed(e.getTrainId(),train.speed);
                }else{
                    tm4.exit();
                }
            }

            //Sensor o(9,13)
            if(e.getXpos()==9 && e.getYpos()==13 && e.getStatus()==e.ACTIVE){
                if(!train.goingDown){
                    sim.setSpeed(e.getTrainId(),0);
                    tm4.enter();
                    sim.setSwitch(3,11,sim.SWITCH_RIGHT);
                    sim.setSpeed(e.getTrainId(),train.speed);
                }else{
                    tm4.exit();
                }
            }

            //Sensor b1(14,3)
            if(e.getXpos()==14 && e.getYpos()==3 && e.getStatus()==e.ACTIVE){
                if(!train.goingDown) trainStop(train);
            }

            //Sensor b2(14,5)
            if(e.getXpos()==14 && e.getYpos()==5 && e.getStatus()==e.ACTIVE){
                if(!train.goingDown){
                    trainStop(train);
                }
            }

            //Sensor b3(14,11)
            if(e.getXpos()==14 && e.getYpos()==11 && e.getStatus()==e.ACTIVE){
                if(train.goingDown) trainStop(train);
            }

            //Sensor b4(14,13)
            if(e.getXpos()==14 && e.getYpos()==13 && e.getStatus()==e.ACTIVE){
                if(train.goingDown){
                    trainStop(train);
                }
            }
        }catch(CommandException err){
            System.err.println(err.getMessage());
            System.exit(1);
        }catch(InterruptedException err){
            System.err.println(err.getMessage());
            System.exit(1);
        }
    }

    //Main function which initiates all values and starts the two threads
    public static void main(String[] args) {
        Lab1 lab = new Lab1(args);
    }

    //Inner Class for Threads
    class Train implements Runnable {
        private int id;
        private int speed;
        private boolean goingDown;
        private boolean bottomStation = false;
        private boolean bottomPath = false;
        private boolean topStationLongPath = false;

        public Train(int id, int speed, boolean goingDown){
            this.id = id;
            this.speed = speed;
            this.goingDown = goingDown;

            try{
                sim.setSpeed(id,speed);
            }catch(CommandException err){
                System.err.println(err.getMessage());
                System.exit(1);
            }
        }

        public void run(){
            try{
                while(true){
                    trainRunning(this, sim.getSensor(id));
                }
            }catch(CommandException err){
                System.err.println(err.getMessage());
                System.exit(1);
            }catch(InterruptedException err){
                System.err.println(err.getMessage());
                System.exit(1);
            }
        }
    }

    class TrainMonitor{
        private final Lock lock = new ReentrantLock();
        private final Condition trackFree = lock.newCondition();
        boolean free = true;

        //Method when entering a monitored section
        public void enter() throws InterruptedException{

            lock.lock();
            while(!free){
                trackFree.await();
            }
            free = false;
            lock.unlock();
        }

        //Method when exiting the monitored section
        public void exit() throws InterruptedException{

            lock.lock();
            free=true;
            trackFree.signal();
            lock.unlock();
        }

        //Method when checking if monitored section is available
        public boolean tryFree() throws InterruptedException{
            lock.lock();
            if(free){
                free=false;
                lock.unlock();
                return true;
            }else{
                lock.unlock();
                return false;
            }
        }
    }
}
