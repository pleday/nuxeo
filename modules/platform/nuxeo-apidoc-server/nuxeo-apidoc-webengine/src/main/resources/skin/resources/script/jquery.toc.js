(function ($) {

  $.fn.toc = function (tocListContainer, tocList) {
    $(tocList).empty();

    let index = 0;
    $("h2").each(function () {
      const name = "toc" + index;
      const text = $(this).text();
      $(this).before(`<a name="${name}" class="anchor"></a>`);
      $(tocList).append(`<li><a href="#${name}">${text}</a></li>`);
      index++;
    });

    if (index < 2) {
      // only one item ==> remove TOC
      $(tocListContainer).remove();
    }

  }

})(jQuery);