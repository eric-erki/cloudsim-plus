package org.cloudbus.cloudsim.examples.network.datacenter;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.network.datacenter.AppCloudlet;
import org.cloudbus.cloudsim.network.datacenter.CloudletSendTask;
import org.cloudbus.cloudsim.network.datacenter.CloudletExecutionTask;
import org.cloudbus.cloudsim.network.datacenter.CloudletReceiveTask;
import org.cloudbus.cloudsim.network.datacenter.CloudletTask;
import org.cloudbus.cloudsim.network.datacenter.NetDatacenterBroker;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkVm;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;

/**
 * An example of a Bag of Tasks {@link AppCloudlet}'s that are composed of
 * 3 {@link NetworkCloudlet}, where 2 of them send data to the first created one,
 * that waits to data be received.
 *
 * @author Saurabh Kumar Garg
 * @author Rajkumar Buyya
 * @author Manoel Campos da Silva Filho
 */
public class NetworkVmsExampleBagOfTasksAppCloudlet extends NetworkVmsExampleAppCloudletAbstract {

    public NetworkVmsExampleBagOfTasksAppCloudlet(){
        super();
    }

    /**
     * Starts the execution of the example.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        new NetworkVmsExampleBagOfTasksAppCloudlet();
    }

    /**
     * @todo @author manoelcampos It isn't adding packets to send.
     * See {@link CloudletSendTask#addPacket(Cloudlet, long)}
     * @param app
     * @param broker
     * @return
     */
    @Override
    public List<NetworkCloudlet> createNetworkCloudlets(AppCloudlet app, NetDatacenterBroker broker){
        final int NETCLOUDLETS_FOR_EACH_APP = 3;
        List<NetworkCloudlet> networkCloudletList = new ArrayList<>(NETCLOUDLETS_FOR_EACH_APP+1);
        List<NetworkVm> selectedVms =
             randomlySelectVmsForAppCloudlet(broker, NETCLOUDLETS_FOR_EACH_APP+1);
        //basically, each task runs the simulation and then data is consolidated in one task
        long memory = 1000;
        long networkCloudletLength = 10000;
        int taskStageId=0;
        int currentCloudletId = -1;
        for(int i = 0; i < NETCLOUDLETS_FOR_EACH_APP; i++){
            currentCloudletId++;
            UtilizationModel utilizationModel = new UtilizationModelFull();
            NetworkCloudlet cloudlet =
                    new NetworkCloudlet(
                            currentCloudletId,
                            networkCloudletLength,
                            NETCLOUDLET_PES_NUMBER);
            cloudlet.setAppCloudlet(app)
                    .setMemory(memory)
                    .setCloudletFileSize(NETCLOUDLET_FILE_SIZE)
                    .setCloudletOutputSize(NETCLOUDLET_OUTPUT_SIZE)
                    .setUtilizationModel(utilizationModel)
                    .setBroker(broker)
                    .setVm(selectedVms.get(i));

            //compute and send data to node 0
            CloudletTask task;
            task = new CloudletExecutionTask(taskStageId++, networkCloudletLength);
            task.setMemory(memory);
            cloudlet.addTask(task);

            //NetworkCloudlet 0 wait data from other cloudlets, while the other cloudlets send data
            if (i==0){
                for(int j=1; j < NETCLOUDLETS_FOR_EACH_APP; j++) {
                    task = new CloudletReceiveTask(taskStageId++, selectedVms.get(j+1).getId());
                    task.setMemory(memory);
                    cloudlet.addTask(task);
                }
            } else {
                task = new CloudletSendTask(taskStageId++);
                task.setMemory(memory);
                cloudlet.addTask(task);
            }

            networkCloudletList.add(cloudlet);
        }

        return networkCloudletList;
    }

}
