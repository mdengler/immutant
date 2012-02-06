/*
 * Copyright 2008-2012 Red Hat, Inc, and individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.immutant.core.processors;

import java.io.File;
import java.util.ArrayList;

import org.immutant.core.ApplicationBootstrapUtils;
import org.immutant.core.ClojureMetaData;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;

public class ProjectCljParsingProcessor implements DeploymentUnitProcessor {
    
    public ProjectCljParsingProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        ClojureMetaData metaData = deploymentUnit.getAttachment( ClojureMetaData.ATTACHMENT_KEY );
        
        if (metaData == null) {
            return;
        }
        
        ResourceRoot resourceRoot = deploymentUnit.getAttachment( Attachments.DEPLOYMENT_ROOT );
        File root;
        try {
            root = resourceRoot.getRoot().getPhysicalFile();
            metaData.setLeinProject( ApplicationBootstrapUtils.readLeinProject( root ) );
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException( e );
        }
        

    }

    @Override
    public void undeploy(DeploymentUnit context) {
       
    }
    
    static final Logger log = Logger.getLogger( "org.immutant.core" );
}
