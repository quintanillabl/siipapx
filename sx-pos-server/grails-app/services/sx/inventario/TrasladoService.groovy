package sx.inventario

import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import sx.core.Folio
import sx.core.Producto
import sx.logistica.Chofer

@Transactional
class TrasladoService {

    SpringSecurityService springSecurityService

    /*
    @Subscriber
    public atender(SolicitudDeTraslado sol ){
        log.debug('Atendiendo solicitud de traslado: ', sol);
        Traslado.withNewTransaction {
            SolicitudDeTraslado origen = SolicitudDeTraslado.get(sol.id)
            generarTpe(origen)
            generarTps(origen)
        }
    }
    */

    @Subscriber
    public atender(Map  map ){
        log.debug('Atendiendo solicitud de traslado: {}', map);
        Traslado.withNewTransaction {
            SolicitudDeTraslado origen = SolicitudDeTraslado.get(map.sol)
            generarTpe(origen)
            generarTps(origen, map.chofer, map.comentario)
        }
    }

    private generarTpe( SolicitudDeTraslado sol) {
        log.debug('Generando TPE para Sol: {}', sol);
        Traslado traslado = new Traslado()
        traslado.sucursal = sol.sucursalSolicita
        traslado.fecha = new Date()
        traslado.documento = sol.documento
        traslado.tipo = 'TPE'
        traslado.comentario = sol.comentario
        traslado.clasificacionVale = sol.clasificacionVale
        traslado.kilos = 0
        traslado.porInventario = false
        traslado.solicitudDeTraslado = sol
        sol.partidas.each { SolicitudDeTrasladoDet solDet ->
            TrasladoDet det = new TrasladoDet()
            det.producto = solDet.producto
            det.kilos = 0
            det.comentario = solDet.comentario
            det.cantidad = solDet.recibido
            det.cortes = solDet.cortes
            det.cortesInstruccion = solDet.cortesInstruccion
            det.solicitado = solDet.solicitado
            traslado.addToPartidas(det)
        }
        logEntity(traslado)
        traslado.save()
        log.debug('TPE generado: {}', traslado)
        return traslado
    }

    private generarTps(SolicitudDeTraslado sol, Chofer chofer, String comentario){
        assert chofer, 'Se requiere el chofer para atender el traslado'
        assert comentario, 'Se requiere el comentario al atender el traslado'
        log.debug('Generando TPS para Sol:{} Chofer:{} comentario: {}', sol.id, chofer.id, comentario);
        Traslado tps = new Traslado()
        tps.sucursal = sol.sucursalAtiende
        tps.fecha = new Date()
        tps.documento = getFolio()
        tps.comentario = comentario
        tps.chofer = chofer
        tps.tipo = 'TPS'
        tps.clasificacionVale = sol.clasificacionVale
        tps.kilos = 0
        tps.porInventario = false
        tps.solicitudDeTraslado = sol
        sol.partidas.each { SolicitudDeTrasladoDet solDet ->
            TrasladoDet det = new TrasladoDet()
            det.producto = solDet.producto
            det.kilos = 0
            det.comentario = solDet.comentario
            det.cantidad = solDet.recibido.abs() * -1
            det.cortes = solDet.cortes
            det.cortesInstruccion = solDet.cortesInstruccion
            det.solicitado = solDet.solicitado
            tps.addToPartidas(det)
        }
        logEntity(tps)
        tps.save()
        log.debug('TPS generado: {}', tps)
        return tps
    }

    def logEntity(Traslado traslado) {
        def user = getUser()
        if(! traslado.id)
            traslado.createUser = user
        traslado.updateUser = user
    }

    def getFolio() {
        return Folio.nextFolio('TRASLADOS','TPS')
    }

    def getUser() {
        /*
        if(springSecurityService) {
            def principal = springSecurityService.getPrincipal()
            return principal.username
        }
        */
        return 'NA'
    }

}