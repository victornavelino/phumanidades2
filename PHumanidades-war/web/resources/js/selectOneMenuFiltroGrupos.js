/*
 * Reemplaza el filtro de p:selectOneMenu (PrimeFaces 5.2) para que funcione con
 * SelectItemGroup: un item se muestra si coincide su texto o el de su grupo
 * (ej: carrera), y los grupos sin items visibles se ocultan.
 * Ademas ignora acentos y mayusculas (si no es caseSensitive).
 */
(function () {
    if (!window.PrimeFaces || !PrimeFaces.widget || !PrimeFaces.widget.SelectOneMenu) {
        return;
    }

    function normalizar(texto, caseSensitive) {
        texto = $.trim(texto || '');
        if (texto.normalize) {
            texto = texto.normalize('NFD').replace(/[̀-ͯ]/g, '');
        }
        return caseSensitive ? texto : texto.toLowerCase();
    }

    PrimeFaces.widget.SelectOneMenu.prototype.filter = function (value) {
        var $this = this,
            caseSensitive = this.cfg.caseSensitive,
            filtro = normalizar(value, caseSensitive),
            lis = this.itemsContainer.children('li');

        this.cfg.initialHeight = this.cfg.initialHeight || this.itemsWrapper.height();

        if (filtro === '') {
            lis.show();
        } else {
            var grupoCoincide = false;
            lis.each(function () {
                var li = $(this),
                    coincide = $this.filterMatcher(normalizar(li.text(), caseSensitive), filtro);

                if (li.hasClass('ui-selectonemenu-item-group')) {
                    grupoCoincide = coincide;
                } else if (grupoCoincide || coincide) {
                    li.show();
                } else {
                    li.hide();
                }
            });

            //ocultamos los grupos que quedaron sin items visibles
            lis.filter('.ui-selectonemenu-item-group').each(function () {
                var grupo = $(this),
                    itemsVisibles = grupo.nextUntil('.ui-selectonemenu-item-group').filter(function () {
                        return this.style.display !== 'none';
                    });
                grupo.toggle(itemsVisibles.length > 0);
            });
        }

        var primero = this.items.filter(':visible:first');
        if (primero.length) {
            this.highlightItem(primero);
        }

        if (this.itemsContainer.height() < this.cfg.initialHeight) {
            this.itemsWrapper.css('height', 'auto');
        } else {
            this.itemsWrapper.height(this.cfg.initialHeight);
        }
        this.alignPanel();
    };
})();
