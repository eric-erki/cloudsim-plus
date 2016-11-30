/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.hosts;

import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmStateHistoryEntry;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.lists.PeList;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;

/**
 * A host supporting dynamic workloads and performance degradation.
 *
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 * @todo @author manoelcampos When using this host, the
 * {@link DatacenterBroker#getCloudletsFinishedList()} returns an empty list
 * after stopping the simulation.
 */
public class HostDynamicWorkloadSimple extends HostSimple implements HostDynamicWorkload {

    /**
     * The utilization mips.
     */
    private double utilizationMips;

    /**
     * The previous utilization mips.
     */
    private double previousUtilizationMips;

    /**
     * The host utilization state history.
     */
    private final List<HostStateHistoryEntry> stateHistory = new LinkedList<>();

    /**
     * Creates a host.
     *
     * @param id the id
     * @param storage the storage capacity
     * @param peList the host's PEs list
     *
     */
    public HostDynamicWorkloadSimple(int id, long storage, List<Pe> peList) {
        super(id, storage, peList);
        setUtilizationMips(0);
        setPreviousUtilizationMips(0);
    }

    /**
     * Creates a host with the given parameters.
     *
     * @param id the id
     * @param ramProvisioner the ram provisioner
     * @param bwProvisioner the bw provisioner
     * @param storage the storage capacity
     * @param peList the host's PEs list
     * @param vmScheduler the VM scheduler
     *
     * @deprecated Use the other available constructors with less parameters
     * and set the remaining ones using the respective setters.
     * This constructor will be removed in future versions.
     */
    @Deprecated
    public HostDynamicWorkloadSimple(
            int id,
            ResourceProvisioner ramProvisioner,
            ResourceProvisioner bwProvisioner,
            long storage,
            List<Pe> peList,
            VmScheduler vmScheduler)
    {
        this(id, storage, peList);
        setRamProvisioner(ramProvisioner);
        setBwProvisioner(bwProvisioner);
        setVmScheduler(vmScheduler);
    }

    @Override
    public double updateVmsProcessing(double currentTime) {
        double smallerTime = super.updateVmsProcessing(currentTime);
        setPreviousUtilizationMips(getUtilizationMips());
        setUtilizationMips(0);
        double hostTotalRequestedMips = 0;

        for (Vm vm : getVmList()) {
            getVmScheduler().deallocatePesForVm(vm);
        }

        for (Vm vm : getVmList()) {
            getVmScheduler().allocatePesForVm(vm, vm.getCurrentRequestedMips());
        }

        for (Vm vm : getVmList()) {
            double totalRequestedMips = vm.getCurrentRequestedTotalMips();
            double totalAllocatedMips = getVmScheduler().getTotalAllocatedMipsForVm(vm);

            if (!Log.isDisabled() && vm.getHost() != Host.NULL) {
                Log.printFormattedLine(
                        "%.2f: [Host #" + getId() + "] Total allocated MIPS for VM #" + vm.getId()
                        + " (Host #" + vm.getHost().getId()
                        + ") is %.2f, was requested %.2f out of total %.2f (%.2f%%)",
                        getSimulation().clock(),
                        totalAllocatedMips,
                        totalRequestedMips,
                        vm.getMips(),
                        totalRequestedMips / vm.getMips() * 100);

                List<Pe> pes = getVmScheduler().getPesAllocatedForVM(vm);
                StringBuilder pesString = new StringBuilder();
                for (Pe pe : pes) {
                    pesString.append(String.format(" PE #" + pe.getId() + ": %.2f.", pe.getPeProvisioner()
                            .getTotalAllocatedMipsForVm(vm)));
                }
                Log.printFormattedLine(
                        "%.2f: [Host #" + getId() + "] MIPS for VM #" + vm.getId() + " by PEs ("
                        + getNumberOfPes() + " * " + getVmScheduler().getPeCapacity() + ")."
                        + pesString,
                        getSimulation().clock());
            }

            if (getVmsMigratingIn().contains(vm)) {
                Log.printFormattedLine("%.2f: [Host #" + getId() + "] VM #" + vm.getId()
                        + " is being migrated to Host #" + getId(), getSimulation().clock());
            } else {
                if (totalAllocatedMips + 0.1 < totalRequestedMips) {
                    Log.printFormattedLine("%.2f: [Host #" + getId() + "] Under allocated MIPS for VM #" + vm.getId()
                            + ": %.2f", getSimulation().clock(), totalRequestedMips - totalAllocatedMips);
                }

                VmStateHistoryEntry entry = new VmStateHistoryEntry(
                        currentTime,
                        totalAllocatedMips,
                        totalRequestedMips,
                        (vm.isInMigration() && !getVmsMigratingIn().contains(vm)));
                vm.addStateHistoryEntry(entry);

                if (vm.isInMigration()) {
                    Log.printFormattedLine(
                            "%.2f: [Host #" + getId() + "] VM #" + vm.getId() + " is in migration",
                            getSimulation().clock());
                    totalAllocatedMips /= 0.9; // performance degradation due to migration - 10%
                }
            }

            setUtilizationMips(getUtilizationMips() + totalAllocatedMips);
            hostTotalRequestedMips += totalRequestedMips;
        }

        addStateHistoryEntry(
                currentTime,
                getUtilizationMips(),
                hostTotalRequestedMips,
                (getUtilizationMips() > 0));

        return smallerTime;
    }

    /**
     * Gets the list of completed vms.
     *
     * @return the completed vms
     */
    @Override
    public List<Vm> getCompletedVms() {
        List<Vm> vmsToRemove = new ArrayList<>();
        for (Vm vm : getVmList()) {
            if (vm.isInMigration()) {
                continue;
            }
            if (vm.getCurrentRequestedTotalMips() == 0) {
                vmsToRemove.add(vm);
            }
        }
        return vmsToRemove;
    }

    /**
     * Gets the max utilization percentage among by all PEs.
     *
     * @return the maximum utilization percentage
     */
    @Override
    public double getMaxUtilization() {
        return PeList.getMaxUtilization(getPeList());
    }

    /**
     * Gets the max utilization percentage among by all PEs allocated to a VM.
     *
     * @param vm the vm
     * @return the max utilization percentage of the VM
     */
    @Override
    public double getMaxUtilizationAmongVmsPes(Vm vm) {
        return PeList.getMaxUtilizationAmongVmsPes(getPeList(), vm);
    }

    /**
     * Gets the utilization of memory (in absolute values).
     *
     * @return the utilization of memory
     */
    @Override
    public long getUtilizationOfRam() {
        return getRamProvisioner().getTotalAllocatedResource();
    }

    /**
     * Gets the utilization of bw (in absolute values).
     *
     * @return the utilization of bw
     */
    @Override
    public long getUtilizationOfBw() {
        return getBwProvisioner().getTotalAllocatedResource();
    }

    /**
     * Get current utilization of CPU in percentage.
     *
     * @return current utilization of CPU in percents
     */
    @Override
    public double getUtilizationOfCpu() {
        double utilization = getUtilizationMips() / getTotalMips();
        if (utilization > 1 && utilization < 1.01) {
            utilization = 1;
        }
        return utilization;
    }

    /**
     * Gets the previous utilization of CPU in percentage.
     *
     * @return the previous utilization of cpu in percents
     */
    @Override
    public double getPreviousUtilizationOfCpu() {
        double utilization = getPreviousUtilizationMips() / getTotalMips();
        if (utilization > 1 && utilization < 1.01) {
            utilization = 1;
        }
        return utilization;
    }

    /**
     * Get current utilization of CPU in MIPS.
     *
     * @return current utilization of CPU in MIPS
     * @todo This method only calls the {@link #getUtilizationMips()}.
     * getUtilizationMips may be deprecated and its code copied here.
     */
    @Override
    public double getUtilizationOfCpuMips() {
        return getUtilizationMips();
    }

    /**
     * Gets the utilization of CPU in MIPS.
     *
     * @return current utilization of CPU in MIPS
     */
    @Override
    public double getUtilizationMips() {
        return utilizationMips;
    }

    /**
     * Sets the utilization mips.
     *
     * @param utilizationMips the new utilization mips
     */
    protected final void setUtilizationMips(double utilizationMips) {
        this.utilizationMips = utilizationMips;
    }

    /**
     * Gets the previous utilization of CPU in mips.
     *
     * @return the previous utilization of CPU in mips
     */
    @Override
    public double getPreviousUtilizationMips() {
        return previousUtilizationMips;
    }

    /**
     * Sets the previous utilization of CPU in mips.
     *
     * @param previousUtilizationMips the new previous utilization of CPU in
     * mips
     */
    protected final void setPreviousUtilizationMips(double previousUtilizationMips) {
        this.previousUtilizationMips = previousUtilizationMips;
    }

    /**
     * Gets the host state history.
     *
     * @return the state history
     */
    @Override
    public List<HostStateHistoryEntry> getStateHistory() {
        return Collections.unmodifiableList(stateHistory);
    }

    /**
     * Adds a host state history entry.
     *
     * @param time the time
     * @param allocatedMips the allocated mips
     * @param requestedMips the requested mips
     * @param isActive the is active
     */
    @Override
    public void addStateHistoryEntry(double time, double allocatedMips, double requestedMips, boolean isActive) {
        HostStateHistoryEntry newState = new HostStateHistoryEntry(
                time,
                allocatedMips,
                requestedMips,
                isActive);
        if (!stateHistory.isEmpty()) {
            HostStateHistoryEntry previousState = stateHistory.get(stateHistory.size() - 1);
            if (previousState.getTime() == time) {
                stateHistory.set(stateHistory.size() - 1, newState);
                return;
            }
        }
        stateHistory.add(newState);
    }

}
