Features and changes under consideration. Not necessarily in order.

* Mutation testing using PIT http://pitest.org/

* Remove Netty dependency in client and protocol. Having a dependency-free client and protocol seems
  worthwhile as it opens up the possibility of use on Android. This would mean using either 
  ByteBuffer or byte[] in place of ByteBuf.

* Further towards potential use on Android, remove the few Java 8 features from the codebase. This
  only makes sense if above the client and protocol changes are made first.

* Maven Build. I avoid Maven because of its magic and tendency to "download the internet". But I
  can't ignore Maven's defacto status as _the_ Java build tool; having a pom will ease integration
  for many potential users.

* Potential optimizations to consider:
  - Extract from fastutil or other primitive collection replacements for LinkedHashMap and TreeMap.
  - Unify byte[] vs ByteBuf. Exclusively using ByteBuf could potentially eliminate copies?
  - Overall garbage generation reduction.

Non-features, will not be implemented:

* Logging in the client and protocol. I have little desire to wade into the tire fire that is Java
  logging. My stance is that it's a choice client code should make.

