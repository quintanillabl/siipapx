package sx.compras

import grails.rest.RestfulController
import grails.plugin.springsecurity.annotation.Secured

import sx.core.Folio
import sx.core.Sucursal
import sx.reports.ReportService

@Secured("ROLE_COMPRAS_USER")
class CompraController extends RestfulController{

    static responseFormats = ['json']

    ReportService reportService;

    CompraController(){
        super(Compra)
    }

    @Override
    protected List listAllResources(Map params) {
        params.sort = 'lastUpdated'
        params.order = 'desc'
        params.max = 50

        def query = Compra.where {sucursal.id == params.sucursal || sucursal.nombre == 'OFICINAS'}

        if(params.boolean('pendientes')){
            query = query.where {pendiente == true}
        }
        if( params.folio) {
            query = query.where {folio == params.int('folio') }
        }
        if(params.term) {
            if(params.term.isInteger()) {
                query = query.where{folio == params.term.toInteger()}
            } else {
                def search = '%' + params.term + '%'
                query = query.where { proveedor.nombre =~ search || comentario =~ search}
            }

        }
        def list = query.list(params)
        return list
    }

    @Override
    protected Object saveResource(Object resource) {
        resource.folio = Folio.nextFolio('COMPRA','OFICINAS')
        resource.createdBy = getPrincipal().username
        return super.saveResource(resource)
    }

    def print( ) {
        //params.ID = params.id;
        def pdf =  reportService.run('OrdenDeCompraSuc.jrxml', params)
        render (file: pdf.toByteArray(), contentType: 'application/pdf', filename: 'OrdenDeCompraSuc.pdf')
    }

}
