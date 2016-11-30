/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.vms.network;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.cloudlets.network.NetworkCloudlet;
import org.cloudbus.cloudsim.network.HostPacket;
import org.cloudbus.cloudsim.vms.Vm;

import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;

/**
 * NetworkVm class extends {@link VmSimple} to support simulation of networked
 * datacenters. It executes actions related to management of packets (sent and
 * received).
 *
 * <br>Please refer to following publication for more details:<br>
 * <ul>
 * <li>
 * <a href="http://dx.doi.org/10.1109/UCC.2011.24">
 * Saurabh Kumar Garg and Rajkumar Buyya, NetworkCloudSim: Modelling Parallel
 * Applications in Cloud Simulations, Proceedings of the 4th IEEE/ACM
 * International Conference on Utility and Cloud Computing (UCC 2011, IEEE CS
 * Press, USA), Melbourne, Australia, December 5-7, 2011.
 * </a>
 * </ul>
 *
 * @author Saurabh Kumar Garg
 * @since CloudSim Toolkit 3.0
 * @todo Attributes should be private
 */
public class NetworkVm extends VmSimple {

    /**
     * List of {@link NetworkCloudlet} of the VM.
     */
    public List<NetworkCloudlet> cloudletList;

    /**
     * List of packets received by the VM.
     */
    public List<HostPacket> receivedPacketList;

    /** Indicates if the VM is free or not. */
    public boolean free;

    /**
     * The time when the VM finished to process its cloudlets.
     */
    public double finishTime;

    /**
     * Creates a NetworkVm with 1024 MB of RAM, 1000 Megabits/s of Bandwidth and 1024 MB of Storage Size.
     *
     * To change these values, use the respective setters. While the Vm {@link #isCreated()
     * is not created inside a Host}, such values can be changed freely.
     *
     * @param id unique ID of the VM
     * @param mipsCapacity the mips capacity of each Vm {@link Pe}
     * @param numberOfPes amount of {@link Pe} (CPU cores)
     *
     * @pre id >= 0
     * @pre numberOfPes > 0
     * @post $none
     */
    public NetworkVm(int id, double mipsCapacity, int numberOfPes) {
        super(id, mipsCapacity, numberOfPes);
        cloudletList = new ArrayList<>();
    }


    /**
     * Creates a NetworkVm with the given parameters.
     *
     * @param id unique ID of the VM
     * @param broker ID of the VM's owner, that is represented by the id of the {@link DatacenterBroker}
     * @param mipsCapacity the mips capacity of each Vm {@link Pe}
     * @param numberOfPes amount of {@link Pe} (CPU cores)
     * @param ramCapacity amount of ram in Megabytes
     * @param bwCapacity amount of bandwidth to be allocated to the VM (in Megabits/s)
     * @param size size the VM image in Megabytes (the amount of storage it will use, at least initially).
     * @param vmm Virtual Machine Monitor that manages the VM lifecycle
     * @param cloudletScheduler scheduler that defines the execution policy for Cloudlets inside this Vm
     *
     * @deprecated Use the other available constructors with less parameters
     * and set the remaining ones using the respective setters.
     * This constructor will be removed in future versions.
     */
    @Deprecated
    public NetworkVm(
            int id,
            DatacenterBroker broker,
            double mipsCapacity,
            int numberOfPes,
            int ramCapacity,
            long bwCapacity,
            long size,
            String vmm,
            CloudletScheduler cloudletScheduler)
    {
        this(id, mipsCapacity, numberOfPes);
        setBroker(broker);
        setRam(ramCapacity);
        setBw(bwCapacity);
        setSize(size);
        setVmm(vmm);
        setCloudletScheduler(cloudletScheduler);
    }

    public boolean isFree() {
        return free;
    }

    @Override
    public int compareTo(Vm o) {
        return Double.compare(this.finishTime, ((NetworkVm) o).finishTime);
    }
}
