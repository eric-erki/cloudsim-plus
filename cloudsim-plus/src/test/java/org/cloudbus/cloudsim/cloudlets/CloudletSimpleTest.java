/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.cloudlets;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterMocker;
import org.cloudbus.cloudsim.mocks.CloudSimMocker;
import org.cloudbus.cloudsim.mocks.MocksHelper;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelStochastic;
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author	Anton Beloglazov
 * @since	CloudSim Toolkit 2.0
 */
public class CloudletSimpleTest {

    public static final int PES_NUMBER = 2;

    private CloudletSimple cloudlet;
    private UtilizationModel utilizationModelCpu;
    private UtilizationModel utilizationModelRam;
    private UtilizationModel utilizationModelBw;

    @Before
    public void setUp() {
        utilizationModelCpu = new UtilizationModelStochastic();
        utilizationModelRam = new UtilizationModelStochastic();
        utilizationModelBw = new UtilizationModelStochastic();
        cloudlet = new CloudletSimple(0, CloudletTestUtil.CLOUDLET_LENGTH, PES_NUMBER);
        cloudlet.setFileSize(CloudletTestUtil.CLOUDLET_FILE_SIZE)
                .setOutputSize(CloudletTestUtil.CLOUDLET_OUTPUT_SIZE)
                .setUtilizationModelCpu(utilizationModelCpu)
                .setUtilizationModelRam(utilizationModelRam)
                .setUtilizationModelBw(utilizationModelBw);
    }

    @Test
    public void testCloudlet() {
        assertEquals(CloudletTestUtil.CLOUDLET_LENGTH, cloudlet.getLength(), 0);
        assertEquals(CloudletTestUtil.CLOUDLET_LENGTH * PES_NUMBER, cloudlet.getTotalLength(), 0);
        assertEquals(CloudletTestUtil.CLOUDLET_FILE_SIZE, cloudlet.getFileSize());
        assertEquals(CloudletTestUtil.CLOUDLET_OUTPUT_SIZE, cloudlet.getOutputSize());
        assertEquals(PES_NUMBER, cloudlet.getNumberOfPes());
        assertSame(utilizationModelCpu, cloudlet.getUtilizationModelCpu());
        assertSame(utilizationModelRam, cloudlet.getUtilizationModelRam());
        assertSame(utilizationModelBw, cloudlet.getUtilizationModelBw());
    }

    @Test
    public void testAddOnCloudletFinishEventListener() {
        final EventListener<CloudletVmEventInfo> listener = (info) -> {};
        cloudlet.addOnFinishListener(listener);
        assertTrue(cloudlet.removeOnFinishListener(listener));
    }

    @Test(expected = NullPointerException.class)
    public void testAddOnCloudletFinishEventListenerWhenNull() {
        cloudlet.addOnFinishListener(null);
    }

    @Test
    public void testRemoveOnCloudletFinishEventListener() {
        final EventListener<CloudletVmEventInfo> listener = (info) -> {};
        cloudlet.addOnFinishListener(listener);
        assertTrue(cloudlet.removeOnFinishListener(listener));
    }

    @Test
    public void testRemoveOnCloudletFinishEventListenerWhenNull() {
        cloudlet.addOnFinishListener(e->{});
        assertFalse(cloudlet.removeOnFinishListener(null));
    }

    @Test
    public void testGetWaitingTime() {
        final double arrivalTime = 0.0, execStartTime = 10.0;
        final CloudSim cloudsim = CloudSimMocker.createMock(mocker -> {
            mocker.clock(arrivalTime);
        });

        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        cloudlet.setBroker(MocksHelper.createMockBroker(cloudsim));
        assertEquals(0, cloudlet.getWaitingTime(), 0);
        cloudlet.assignToDatacenter(Datacenter.NULL);
        final double expectedWaitingTime = execStartTime - arrivalTime;
        cloudlet.registerArrivalInDatacenter();
        cloudlet.setExecStartTime(execStartTime);
        assertEquals(expectedWaitingTime, cloudlet.getWaitingTime(), 0);
    }

    @Test
    public void testAssignCloudletToDataCenterWhenRecordLogEnabledDatacenterNotAssigned() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet(0);
        cloudlet.assignToDatacenter(Datacenter.NULL);
        assertEquals(Datacenter.NULL, cloudlet.getLastDatacenter());
    }

    @Test
    public void testAssignCloudletToDataCenterWhenRecordLogEnabledDatacenterAlreadyAssigned() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet(0);
        cloudlet.assignToDatacenter(Datacenter.NULL);

        cloudlet.assignToDatacenter(Datacenter.NULL);
        assertEquals(Datacenter.NULL, cloudlet.getLastDatacenter());
    }

    @Test
    public void testGetExecStartTime() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        assertEquals(0, cloudlet.getExecStartTime(), 0);

        cloudlet.assignToDatacenter(Datacenter.NULL);
        final int execStartTime = 10;
        cloudlet.registerArrivalInDatacenter();
        cloudlet.setExecStartTime(execStartTime);
        assertEquals(execStartTime, cloudlet.getExecStartTime(), 0);
    }

    @Test
    public void testGetDatacenterArrivalTime() {
        final double submissionTime = 1;
        final CloudSim cloudsim = CloudSimMocker.createMock(mocker -> {
            mocker.clock(submissionTime);
        });

        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        cloudlet.setBroker(MocksHelper.createMockBroker(cloudsim));
        assertEquals(Cloudlet.NOT_ASSIGNED, cloudlet.getLastDatacenterArrivalTime(), 0);

        cloudlet.assignToDatacenter(Datacenter.NULL);
        cloudlet.registerArrivalInDatacenter();
        assertEquals(submissionTime, cloudlet.getLastDatacenterArrivalTime(), 0);
    }

    @Test
    public void testGetWallClockTime() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        assertEquals(0, cloudlet.getWallClockTimeInLastExecutedDatacenter(), 0);

        cloudlet.assignToDatacenter(Datacenter.NULL);
        final double arrivalTime = 0.0, execStartTime = 10.0;
        CloudSimMocker.createMock(mocker -> mocker.clock(arrivalTime));

        cloudlet.registerArrivalInDatacenter();
        cloudlet.setExecStartTime(execStartTime);
        final double wallClockTime = execStartTime + 20.0;
        cloudlet.setWallClockTime(wallClockTime, wallClockTime);
        assertEquals(wallClockTime, cloudlet.getWallClockTimeInLastExecutedDatacenter(), 0);
    }

    @Test
    public void testGetActualCPUTime() {
        final double submissionTime = 0, execStartTime = 10;
        final double simulationClock = 100;
        final double actualCpuTime = simulationClock - execStartTime;

        final CloudSim cloudsim = CloudSimMocker.createMock(mocker -> {
            mocker.clock(submissionTime);
            mocker.clock(simulationClock);
        });

        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        cloudlet.setBroker(MocksHelper.createMockBroker(cloudsim));
        assertEquals(Cloudlet.NOT_ASSIGNED, cloudlet.getActualCpuTime(), 0);

        cloudlet.assignToDatacenter(Datacenter.NULL);
        cloudlet.registerArrivalInDatacenter();
        cloudlet.setExecStartTime(execStartTime);
        cloudlet.setStatus(Cloudlet.Status.SUCCESS);
        assertEquals(actualCpuTime, cloudlet.getActualCpuTime(), 0);

        EasyMock.verify(cloudsim);
    }

    @Test
    public void testGetProcessingCost() {
        final double costPerCpuSec = 4, costPerByteOfBw = 2;
        final Datacenter dc = DatacenterMocker.createMock(mocker -> {
            mocker.getCharacteristics().times(2);
            mocker.getCostPerSecond(costPerCpuSec).once();
            mocker.getCostPerBw(costPerByteOfBw).once();
        });

        final Cloudlet cloudlet = CloudletTestUtil.createCloudlet(0, 10000, 2);
        final double inputTransferCost = CloudletTestUtil.CLOUDLET_FILE_SIZE * costPerByteOfBw;
        final double outputTransferCost = CloudletTestUtil.CLOUDLET_OUTPUT_SIZE * costPerByteOfBw;

        final double cpuCost = 40;
        final double totalCost = inputTransferCost + cpuCost + outputTransferCost;
        cloudlet.assignToDatacenter(dc);
        cloudlet.setWallClockTime(10, 10);
        assertEquals(totalCost, cloudlet.getTotalCost(), 0);
    }

    @Test
    public void testGetPriority() {
        final int expected = 8;
        cloudlet.setPriority(expected);
        assertEquals(expected, cloudlet.getPriority(), 0);
    }

    @Test
    public void testAddCloudletFinishedSoFar() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        assertEquals(0, cloudlet.getFinishedLengthSoFar(), 0);

        cloudlet.assignToDatacenter(Datacenter.NULL);
        final long cloudletFinishedSoFar = cloudlet.getLength() / 2;
        assertTrue(cloudlet.addFinishedLengthSoFar(cloudletFinishedSoFar));
        assertEquals(cloudletFinishedSoFar, cloudlet.getFinishedLengthSoFar(), 0);
        assertFalse(cloudlet.addFinishedLengthSoFar(-1));
        assertEquals(cloudletFinishedSoFar, cloudlet.getFinishedLengthSoFar(), 0);
    }

    @Test
    public void testAddCloudletFinishedSoFarWhenValueIsLowerThanLen() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        cloudlet.assignToDatacenter(Datacenter.NULL);
        final long cloudletFinishedSoFar = cloudlet.getLength() / 2;
        assertTrue(cloudlet.addFinishedLengthSoFar(cloudletFinishedSoFar));
        assertEquals(cloudletFinishedSoFar, cloudlet.getFinishedLengthSoFar(), 0);
    }

    @Test
    public void testAddCloudletFinishedSoFarWhenValueIsHigherThanLen() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        cloudlet.assignToDatacenter(Datacenter.NULL);
        final long cloudletFinishedSoFar = cloudlet.getLength() / 2;
        cloudlet.addFinishedLengthSoFar(cloudletFinishedSoFar);
        cloudlet.addFinishedLengthSoFar(cloudletFinishedSoFar*3);
        assertEquals(cloudlet.getLength(), cloudlet.getFinishedLengthSoFar(), 0);
    }

    @Test
    public void testAddCloudletFinishedSoFarWhenLengthParamGreaterThanCloudletLength() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        final long expected = cloudlet.getLength();
        cloudlet.addFinishedLengthSoFar(expected*2);
        assertEquals(expected, cloudlet.getLength(), 0);
    }

    @Test
    public void testGetDatacenterId() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet(0);
        assertEquals(Datacenter.NULL, cloudlet.getLastDatacenter());

        cloudlet.assignToDatacenter(Datacenter.NULL);
        assertEquals(Datacenter.NULL, cloudlet.getLastDatacenter());
    }

    @Test
    public void testGetCostPerSec() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        assertEquals(0, cloudlet.getCostPerSec(), 0);

        cloudlet.assignToDatacenter(Datacenter.NULL);
        assertEquals(0, cloudlet.getCostPerSec(), 0);
    }

    @Test
    public void testSetValidCloudletLength() {
        final int expected = 1000;
        cloudlet.setLength(expected);
        assertEquals(expected, cloudlet.getLength());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetCloudletLengthToZero() {
        cloudlet.setLength(0);
    }

    @Test
    public void testSetCloudletLengthToNegative() {
        final long len = -1000;
        cloudlet.setLength(len);
        assertEquals(len, cloudlet.getLength());
    }

    @Test
    public void testSetValidNumberOfPes() {
        final int expected = 2;
        cloudlet.setNumberOfPes(expected);
        assertEquals(expected, cloudlet.getNumberOfPes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNumberOfPesToZero() {
        cloudlet.setNumberOfPes(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNumberOfPesToNegative() {
        cloudlet.setNumberOfPes(-1);
    }

    @Test
    public void testSetNetServiceLevel() {
        int valid = 1;
        final String trueMsg = "Cloudlet.setNetServiceLevel should return true";
        final String falseMsg = "Cloudlet.setNetServiceLevel should return false";

        assertTrue(trueMsg, cloudlet.setNetServiceLevel(valid));
        assertEquals(valid, cloudlet.getNetServiceLevel());

        final int invalid0 = 0;
        assertFalse(falseMsg, cloudlet.setNetServiceLevel(invalid0));
        assertEquals(valid, cloudlet.getNetServiceLevel());

        final int invalidNegative = -1;
        assertFalse(falseMsg, cloudlet.setNetServiceLevel(invalidNegative));
        assertEquals(valid, cloudlet.getNetServiceLevel());

        valid = 2;
        assertTrue(trueMsg, cloudlet.setNetServiceLevel(valid));
        assertEquals(valid, cloudlet.getNetServiceLevel());
    }

    @Test
    public void testSetUtilizationModels() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        assertNotNull(cloudlet.getUtilizationModelCpu());
        assertNotNull(cloudlet.getUtilizationModelRam());
        assertNotNull(cloudlet.getUtilizationModelBw());
    }

    @Test(expected = NullPointerException.class)
    public void testSetUtilizationModelBwWhenNull() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        cloudlet.setUtilizationModelBw(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetUtilizationModelRamWhenNull() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        cloudlet.setUtilizationModelRam(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetUtilizationModelCpuWhenNull() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        cloudlet.setUtilizationModelCpu(null);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWhenNullUtilizationModel() {
        CloudletTestUtil.createCloudlet(0, null);
    }

    @Test
    public void testSetExecParam() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        //Cloudlet has not assigned to a datacenter yet
        assertFalse(cloudlet.setWallClockTime(1, 2));

        //Assign cloudlet to a datacenter
        cloudlet.assignToDatacenter(Datacenter.NULL);
        assertTrue(cloudlet.setWallClockTime(1, 2));
    }

    @Test
    public void testSetCloudletStatus() {
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();
        cloudlet.setStatus(CloudletSimple.Status.INSTANTIATED);
        //The status is the same of the current cloudlet status (the request has not effect)
        assertFalse(cloudlet.setStatus(CloudletSimple.Status.INSTANTIATED));

        //Actually changing to a new status
        assertTrue(cloudlet.setStatus(CloudletSimple.Status.QUEUED));

        final CloudletSimple.Status newStatus = CloudletSimple.Status.CANCELED;
        assertTrue(cloudlet.setStatus(newStatus));
        assertEquals(newStatus, cloudlet.getStatus());

        //Trying to change to the same current status (the request has not effect)
        assertFalse(cloudlet.setStatus(newStatus));
    }


    @Test
    public void testGetCloudletFinishedSoFar() {
        final long length = 1000;
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();

        assertEquals(0, cloudlet.getFinishedLengthSoFar());

        cloudlet.assignToDatacenter(Datacenter.NULL);
        final long finishedSoFar = length / 10;
        cloudlet.addFinishedLengthSoFar(finishedSoFar);
        assertEquals(finishedSoFar, cloudlet.getFinishedLengthSoFar());

        cloudlet.addFinishedLengthSoFar(length);
        assertEquals(length, cloudlet.getFinishedLengthSoFar());
    }

    @Test
    public void testIsFinished() {
        final long length = 1000;
        final CloudletSimple cloudlet = CloudletTestUtil.createCloudlet();

        assertFalse(cloudlet.isFinished());

        cloudlet.assignToDatacenter(Datacenter.NULL);
        final long finishedSoFar = length / 10;
        cloudlet.addFinishedLengthSoFar(finishedSoFar);
        assertFalse(cloudlet.isFinished());

        cloudlet.addFinishedLengthSoFar(length);
        assertTrue(cloudlet.isFinished());
    }

    @Test
    public void testSetPriority() {
        final int zero = 0;
	    cloudlet.setPriority(zero);
        assertEquals(zero, cloudlet.getPriority());

        final int negative = -1;
        cloudlet.setPriority(negative);
	    assertEquals(negative, cloudlet.getPriority());

        final int one = 1;
        cloudlet.setPriority(one);
	    assertEquals(one, cloudlet.getPriority());
    }

    @Test
    public void testGetUtilizationOfCpu() {
        assertEquals(utilizationModelCpu.getUtilization(0), cloudlet.getUtilizationOfCpu(0), 0);
    }

    @Test
    public void testGetUtilizationOfRam() {
        assertEquals(utilizationModelRam.getUtilization(0), cloudlet.getUtilizationOfRam(0), 0);
    }

    @Test
    public void testGetUtilizationOfBw() {
        assertEquals(utilizationModelBw.getUtilization(0), cloudlet.getUtilizationOfBw(0), 0);
    }
}
