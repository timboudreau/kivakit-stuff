[//]: # (start-user-text)

<a href="https://www.kivakit.org">
<img src="https://www.kivakit.org/images/web-32.png" srcset="https://www.kivakit.org/images/web-32-2x.png 2x"/>
</a>
&nbsp;
<a href="https://twitter.com/openkivakit">
<img src="https://www.kivakit.org/images/twitter-32.png" srcset="https://www.kivakit.org/images/twitter-32-2x.png 2x"/>
</a>
&nbsp;
<a href="https://kivakit.zulipchat.com">
<img src="https://www.kivakit.org/images/zulip-32.png" srcset="https://www.kivakit.org/images/zulip-32-2x.png 2x"/>
</a>

[//]: # (end-user-text)

# kivakit-service &nbsp;&nbsp; <img src="https://www.kivakit.org/images/gears-32.png" srcset="https://www.kivakit.org/images/gears-32-2x.png 2x"/>

This module contains modules for registering and looking up services.

<img src="https://www.kivakit.org/images/horizontal-line-512.png" srcset="https://www.kivakit.org/images/horizontal-line-512-2x.png 2x"/>

[//]: # (start-user-text)

### Summary <a name = "summary"></a>

This project enables service *registration* and *discovery*. A service, in this context,
is any application assigned a port on a particular host through registration. Services
are visible for discovery within a particular scope (local, cluster or network) and
service discovery can also be limited to a scope (all the services on the local host
or all the services on the network). Services also provide metadata, including a type
and version, which allows further scoping of searches. The protocol for the service
is not defined by this API.

*Servers*

1. Register services on a dynamically assigned port on the local host
2. Provide metadata for those services to guide discovery

*Clients*

1. Discover services within a scope (local, cluster, local area network)
2. Provide criteria for discovery (service type and version)

Both servers and clients use the
[*ServiceRegistryClient*](https://github.com/Telenav/kivakit/blob/master/kivakit-service/client/src/main/java/com/telenav/kivakit/service/registry/client/ServiceRegistryClient.java)
to perform these tasks. 

The *server* module contains the *ServiceRegistryServer* application, which provides
registration and discovery for clients, both locally, and on a cluster or the local area
network as a whole. The application is launched automatically by the *ServiceRegistryClient*
if it is not already running. A service registry server for a local area network or cluster
needs to be set up manually.

<img src="https://www.kivakit.org/images/horizontal-line-128.png" srcset="https://www.kivakit.org/images/horizontal-line-128-2x.png 2x"/>

[//]: # (end-user-text)

### Projects <a name = "projects"></a> &nbsp; <img src="https://www.kivakit.org/images/gears-32.png" srcset="https://www.kivakit.org/images/gears-32-2x.png 2x"/>

[**kivakit-service-client**](client/README.md)  
[**kivakit-service-registry**](registry/README.md)  
[**kivakit-service-server**](server/README.md)  
[**kivakit-service-viewer**](viewer/README.md)  

<img src="https://www.kivakit.org/images/horizontal-line-128.png" srcset="https://www.kivakit.org/images/horizontal-line-128-2x.png 2x"/>

### Javadoc Coverage <a name = "javadoc-coverage"></a> &nbsp; <img src="https://www.kivakit.org/images/bargraph-32.png" srcset="https://www.kivakit.org/images/bargraph-32-2x.png 2x"/>

&nbsp; <img src="https://www.kivakit.org/images/meter-80-96.png" srcset="https://www.kivakit.org/images/meter-80-96-2x.png 2x"/>
 &nbsp; &nbsp; [**kivakit-service-client**](client/README.md)  
&nbsp; <img src="https://www.kivakit.org/images/meter-100-96.png" srcset="https://www.kivakit.org/images/meter-100-96-2x.png 2x"/>
 &nbsp; &nbsp; [**kivakit-service-registry**](registry/README.md)  
&nbsp; <img src="https://www.kivakit.org/images/meter-90-96.png" srcset="https://www.kivakit.org/images/meter-90-96-2x.png 2x"/>
 &nbsp; &nbsp; [**kivakit-service-server**](server/README.md)  
&nbsp; <img src="https://www.kivakit.org/images/meter-30-96.png" srcset="https://www.kivakit.org/images/meter-30-96-2x.png 2x"/>
 &nbsp; &nbsp; [**kivakit-service-viewer**](viewer/README.md)

[//]: # (start-user-text)



[//]: # (end-user-text)

<img src="https://www.kivakit.org/images/horizontal-line-512.png" srcset="https://www.kivakit.org/images/horizontal-line-512-2x.png 2x"/>

<sub>Copyright &#169; 2011-2021 [Telenav](https://telenav.com), Inc. Distributed under [Apache License, Version 2.0](LICENSE)</sub>  
<sub>This documentation was generated by [Lexakai](https://www.lexakai.org). UML diagrams courtesy of [PlantUML](https://plantuml.com).</sub>
