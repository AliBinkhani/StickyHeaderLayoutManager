1.Support use in ConcatAdapter and ConcatAdapter nested ConcatAdapter
2.Fixed findLastVisiablexxx method

Use in ConcatAdapter example:
        StickyHeadersLinearLayoutManager stickyHeadersLinearLayoutManager = new StickyHeadersLinearLayoutManager(recyclerView.getContext());
        stickyHeadersLinearLayoutManager.setStickyHeaderProvider(new StickyHeaderProvider() {
            @Override
            public boolean isStickyHeader(RecyclerView.Adapter<?> adapter, int position) {
                //use brv example
                return adapter instanceof BindingAdapter && ((BindingAdapter)adapter).getModel(position) instanceof ItemHover && ((ItemHover)((BindingAdapter)adapter).getModel(position)).getItemHover();
            }
        });
