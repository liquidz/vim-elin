function! elin#internal#clojure#get_ns_form() abort
  let line = getline(1)
  if line ==# '(ns' || line[0:3] ==# '(ns '
		return elin#compat#sexp#get_list(1, 1)
  elseif line ==# '(in-ns' || line[0:7] ==# '(in-ns '
		return elin#compat#sexp#get_list(1, 1)
  else
    let p = getcurpos()
    try
      let [l1, c1] = searchpos('(ns[ \r\n]', 'n')
      let [l2, c2] = searchpos('(in-ns[ \r\n]', 'n')
      if l1 == 0 && l2 == 0
        return ''
      elseif l1 != 0 && l2 == 0
		    return elin#compat#sexp#get_list(l1, c1)
      elseif l1 == 0 && l2 != 0
		    return elin#compat#sexp#get_list(l2, c2)
      elseif l1 < l2
		    return elin#compat#sexp#get_list(l1, c1)
      else
		    return elin#compat#sexp#get_list(l2, c2)
      endif
    finally
      call setpos('.', p)
    endtry
  endif
endfunction