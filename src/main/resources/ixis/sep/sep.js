var System = java.lang.System;

var iri = com.iota.iri;
var Callable = iri.service.CallableRequest;
var Response = iri.service.dto.IXIResponse;
var ErrorResponse = iri.service.dto.ErrorResponse;

var snapshotProvider 	= IOTA.snapshotProvider;

// Log using logger of the ixi class
var log = org.slf4j.LoggerFactory.getLogger(iri.IXI.class);

/*
curl http://localhost:14265 -X POST -H 'X-IOTA-API-Version: 1.4.1' -H 'Content-Type: application/json' -d '{"command": "sep.getSep"}'
*/
function generate(request) {
    return Response.create({
        sep: snapshotProvider.getLatestSnapshot().getSolidEntryPoints(),
        index: snapshotProvider.getLatestSnapshot().getIndex(),
        sepOld: snapshotProvider.getInitialSnapshot().getSolidEntryPoints(),
        indexOld: snapshotProvider.getInitialSnapshot().getIndex()
    });
}

API.put("getSep", new Callable({ call: generate }))
