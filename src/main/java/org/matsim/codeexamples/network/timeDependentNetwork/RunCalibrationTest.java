/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.codeexamples.network.timeDependentNetwork;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.FileReader;
import java.io.BufferedReader;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.codeexamples.scoring.example16customscoring.PersonLeavesFHVEvent;
import org.matsim.codeexamples.scoring.example16customscoring.PersonLeavesTaxiEvent;
import org.matsim.codeexamples.scoring.example16customscoring.PersonParkingEvent;
import org.matsim.codeexamples.scoring.example16customscoring.RunCustomScoringExampleTaxi.TaxiScoringFunctionFactory;
import org.matsim.codeexamples.scoring.example16customscoring.TollPersonEvent1;
import org.matsim.codeexamples.scoring.example16customscoring.TollPersonEvent2;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingModule;
import org.matsim.roadpricing.RoadPricingSchemeUsingTollFactor;
import org.matsim.roadpricing.TollFactor;
import org.matsim.vehicles.Vehicle;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;


/**
 * @author nagel
 *
 */
public class RunCalibrationTest {

	/** 
	 * @param args
	 */
	public static String [] toll1 = {"320888283_0","60325668_0","415882710_0","232473896_0","49032026_0","644974431_0","360639917_1"};
	public static String [] toll2 = {"40596823_0","25154607_0","40595084_0","375623292_0","25464382_0","5669174_0","90086370_0","5699313_0","413749473_0","5681925_0","11878036_0","46469739_0"};

	
	static class ScoreEngine implements PersonEntersVehicleEventHandler,PersonLeavesVehicleEventHandler, LinkLeaveEventHandler {

		private EventsManager eventsManager;

		private Map<Id<Vehicle>, Id<Person>> vehicle2driver = new HashMap<>();

		@Inject
		ScoreEngine(EventsManager eventsManager) {
			this.eventsManager = eventsManager;
			this.eventsManager.addHandler(this);
		}

		@Override 
		public void reset(int iteration) {}
		
		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			
			vehicle2driver.put(event.getVehicleId(), event.getPersonId());
		}

		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			
			if (parkingAt(event.getTime(), event.getPersonId(),event.getVehicleId())) {
				eventsManager.processEvent(new PersonParkingEvent(event.getTime(), vehicle2driver.get(event.getVehicleId())));
				//System.out.println("park");
			}else if (TaxiAt(event.getTime(),event.getVehicleId())) {
				eventsManager.processEvent(new PersonLeavesTaxiEvent(event.getTime(), vehicle2driver.get(event.getVehicleId())));
				//System.out.println("taxi");
			}else if (FHVAt(event.getTime(),event.getVehicleId())) {
				eventsManager.processEvent(new PersonLeavesFHVEvent(event.getTime(), vehicle2driver.get(event.getVehicleId())));
				//System.out.println("FHV");
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if (TollAt(event.getTime(), event.getLinkId())==1) {
				eventsManager.processEvent(new TollPersonEvent1(event.getTime(), vehicle2driver.get(event.getVehicleId())));
				
				//System.out.println("vehicleID:"+vehicle2driver.get(event.getVehicleId()));
			}else if(TollAt(event.getTime(), event.getLinkId())==2){
				eventsManager.processEvent(new TollPersonEvent2(event.getTime(), vehicle2driver.get(event.getVehicleId())));
			}
		}
		
		//private boolean parkingAt_1(double time, Id<Link> linkId){
			
		//}
		// It starts raining on link 1 at 7:30.
		private boolean parkingAt(double time, Id<Person> personId, Id<Vehicle> vehicleId) {
			
			if (time > (0.0 * 60.0 * 60.0) && personId.equals(vehicleId)) {
				return true;
			} else {
				return false;
			}
			
		}
			
		private boolean TaxiAt(double time, Id<Vehicle> vehicleId) {
			
			if (time > (0.0 * 60.0 * 60.0) && vehicleId.toString().contains("taxi")) {
				return true;
			} else {
				return false;
			}
			
		}

		private boolean FHVAt(double time, Id<Vehicle> vehicleId) {
	
			if (time > (0.0 * 60.0 * 60.0) && vehicleId.toString().contains("FHV")) {
				return true;
			} else {
				return false;
			}
	
		}	
		
		private int TollAt(double time, Id<Link> LinkId) {
			List<String> list1=Arrays.asList(toll1);
			List<String> list2=Arrays.asList(toll2);
			int i = 0;
			if (list1.contains(LinkId.toString())) {
				//System.out.println(LinkId.toString());
				i = 1;
			} else if(list2.contains(LinkId.toString())){
				i = 2;
			}
			return i;
		}

	

	}
	
	public static void main(String[] args) {
		//URL configurl = IOUtils.newUrl( ExamplesUtils.getTestScenarioURL("equil") , "config.xml" ) ;
		
		double ExpressFactor []= new double [6];
		double ArterialFactor []= new double [6];
		double ExpressFactor1 []= new double [6];
		double ArterialFactor1 []= new double [6];  
		
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		// "materialize" the road pricing config group:
		//RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class) ;
				
		// configure the time variant network here:
		config.network().setTimeVariantNetwork(true);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		//config.qsim().setPcuThresholdForFlowCapacityEasing(0.03);
		
		// ---
		
		// create/load the scenario here.  The time variant network does already have to be set at this point
		// in the config, otherwise it will not work.
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// ---
	
		// define the toll factor as an anonymous class.  If more flexibility is needed, convert to "full" class.
		
		/*
		TollFactor tollFactor = new TollFactor(){
			@Override public double getTollFactor(Id<Person> personId, Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
				//VehicleType someVehicleType = null ; // --> replace by something meaningful <--

		       // String type = scenario.getVehicles().getTransitVehicles().getVehicles().get( vehicleId ).getType().getDescription();

		                if ( vehicleId.toString().contains("bus")||vehicleId.toString().contains("subway") ||vehicleId.toString().contains("rail") ) {

		                                return 0 ;

		                } else {

		                                return 1 ;
						}
					}
				} ;
				// instantiate the road pricing scheme, with the toll factor inserted:
		RoadPricingSchemeUsingTollFactor scheme = new RoadPricingSchemeUsingTollFactor(rpConfig.getTollLinksFile(), tollFactor) ;
		*/
				
				
		try{
			BufferedReader reader = new BufferedReader(new FileReader("D:\\yh1995\\Calibration\\new_parameter_20191104.csv"));
			reader.readLine();
			String line=null;
			int i = 0;
			double [][] param = new double [24][3];
			double threshold = 0.6;
			while((line=reader.readLine())!=null){
				String temp [] = line.split(",");
				param[i][0]=(Double.parseDouble(temp[1])>threshold?Double.parseDouble(temp[1]):threshold);
				param[i][1]=(Double.parseDouble(temp[3])>threshold?Double.parseDouble(temp[3]):threshold);
				param[i][2]=(Double.parseDouble(temp[4])>threshold?Double.parseDouble(temp[4]):threshold);
				i+=1;
			}
			int col=2;
			for(int j=0;j<6;j++ ){
				ExpressFactor[j]=param[j][col];
				ArterialFactor[j]=param[j+6][col];
				ExpressFactor1[j]=param[j+12][col];
				ArterialFactor1[j]=param[j+18][col];
				System.out.println(ExpressFactor[j]+","+ArterialFactor[j]+","+ExpressFactor1[j]+","+ArterialFactor1[j]);
			}
			
			reader.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			double speed = link.getFreespeed() ;
			//final double threshold = 5./3.6;
			double capacity = link.getCapacity() ;
			Set<String> linkType = link.getAllowedModes();
			//if ( speed > threshold ) {
			
			// 1st loop
//			double ExpressFactor []= {0.133806757,0.133806757,0.133806757,0.133806757,0.133806757,1};
//			double ArterialFactor []= {0.133806757,0.133806757,0.133806757,1,0.133806757,0.133806757};
//			double ExpressFactor1 []= {1,1,0.133806757,1,1,1};
//			double ArterialFactor1 []= {0.133806757,1,0.133806757,0.133806757,0.133806757,1}; // first
			
//			double ExpressFactor []= {1,1,1,1,1,0.133806757};
//			double ArterialFactor []= {1,1,1,0.133806757,1,1};
//			double ExpressFactor1 []= {0.133806757,0.133806757,1,0.133806757,0.133806757,0.133806757};
//			double ArterialFactor1 []= {1,0.133806757,1,1,1,0.133806757};
//			

			// 2nd loop
//			double ExpressFactor []= {1,0.552512655,0.552512655,1,0.552512655,0.547487345};
//			double ArterialFactor []= {1,1,1,0.1,1,0.552512655};
//			double ExpressFactor1 []= {0.547487345,0.1,1,0.547487345,0.1,0.547487345};
//			double ArterialFactor1 []= {0.552512655,0.1,1,1,1,0.547487345 }; 
			/*
			double ExpressFactor []= {0.552512655,1,1,0.552512655,1,0.1};
			double ArterialFactor []= {0.552512655,0.552512655,0.552512655,0.547487345,0.552512655,1};
			double ExpressFactor1 []= {0.1,0.547487345,0.552512655,0.1,0.547487345,0.1};
			double ArterialFactor1 []= {1,0.547487345,0.552512655,0.552512655,0.552512655,0.1};
			*/
						
/*
			double ExpressFactor []= {1,0.1,0.1,1,0.1,1};
			double ArterialFactor []= {1,1,1,1,1,0.1};
			double ExpressFactor1 []= {1,0.1,1,1,0.1,1};
			double ArterialFactor1 []= {1,0.1,1,1,1,1 }; 
			*/
			
			
			/*
			double ExpressFactor []= {1,0.53467228,0.53467228,1,0.4,0.56532772};
			double ArterialFactor []= {0.56532772,0.56532772,1,0.53467228,0.56532772,0.4};
			double ExpressFactor1 []= {1,0.4,1,1,0.4,0.56532772};
			double ArterialFactor1 []= {0.53467228,0.4,0.56532772,0.56532772,0.56532772,0.56532772}; 
					*/	


			
			if(linkType.contains("car")){
				if ( speed > 33 ) {
					{
						NetworkChangeEvent event = new NetworkChangeEvent(0.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ExpressFactor[5] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ExpressFactor1[5] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(7.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ExpressFactor[0] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ExpressFactor1[0] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(10.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ExpressFactor[1] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ExpressFactor1[1] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(13.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ExpressFactor[2] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ExpressFactor1[2] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(16.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ExpressFactor[3] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ExpressFactor1[3] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(19.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ExpressFactor[4] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ExpressFactor1[4] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(22.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ExpressFactor[5] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ExpressFactor1[5] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
				}else{
					{
						NetworkChangeEvent event = new NetworkChangeEvent(0.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ArterialFactor[5] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ArterialFactor1[5] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(7.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ArterialFactor[0] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ArterialFactor1[0] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(10.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ArterialFactor[1] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ArterialFactor1[1] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(13.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ArterialFactor[2] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ArterialFactor1[2] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(16.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ArterialFactor[3] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ArterialFactor1[3] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(19.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ArterialFactor[4] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ArterialFactor1[4] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
					{
						NetworkChangeEvent event = new NetworkChangeEvent(22.*3600.) ;
						//event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
						event.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity/3600*ArterialFactor[5] ));
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, speed*ArterialFactor1[5] ));
						event.addLink(link);
						NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),event);
					}
				}
			}
			}
			
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new SwissRailRaptorModule());
		//controler.addOverridingModule( new RoadPricingModule( scheme ) ) ;

		//controler.getConfig().controler().setOutputDirectory("D:\\yh1995\\Calibration\\start-1st-loop-".concat("2-lb0.6"));
		
		
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// We add a class which reacts on people who enter a link and lets it rain on them
				// if we are within a certain time window.
				// The class registers itself as an EventHandler and also produces events by itself.
				
				bind(ScoreEngine.class).asEagerSingleton();
				//bind(TaxiEngine.class).asEagerSingleton();
			}
		});

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				
				this.bindScoringFunctionFactory().toInstance(new TaxiScoringFunctionFactory(scenario) ) ;
				//this.bindScoringFunctionFactory().toInstance(new TaxiScoringFunctionFactory(scenario) ) ;
			}

		});
		
		/*
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
			}
		} );
		*/
		controler.run() ;
		
	}

}
