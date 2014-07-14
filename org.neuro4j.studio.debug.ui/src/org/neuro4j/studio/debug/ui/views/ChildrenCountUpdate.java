/*
 * Copyright (c) 2013-2014, Neuro4j.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neuro4j.studio.debug.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.jface.viewers.TreePath;

/**
 * @since 3.3
 */
class ChildrenCountUpdate extends ViewerUpdateMonitor implements IChildrenCountUpdate {

    /**
     * Child count result.
     */
    private int fCount = 0;

    /**
     * Other child count updates for the same content provider. Coalesced requests are
     * batched together into an array.
     */
    private List fBatchedRequests = null;

    /**
     * Flag whether filtering is enabled in viewer. If filtering is enabled, then a
     * children update is performed on child elements to filter them as part of the
     * child count calculation.
     */
    private boolean fShouldFilter = false;

    /**
     * Children indexes which are currently filtered. When updating child count, also need
     * to verify that currently filtered children are still filtered.
     */
    private int[] fFilteredChildren = null;

    /**
     * Children update used to filter children.
     */
    private ChildrenUpdate fChildrenUpdate;

    /**
     * Constructor
     * 
     * @param provider
     *        the content provider to use for the update
     * @param viewerInput
     *        the current input
     * @param elementPath
     *        the path of the element to update
     * @param element
     *        the element to update
     * @param elementContentProvider
     *        the content provider for the element
     */
    public ChildrenCountUpdate(TreeModelContentProvider provider, Object viewerInput, TreePath elementPath, Object element, IElementContentProvider elementContentProvider) {
        super(provider, viewerInput, elementPath, element, elementContentProvider, provider.getPresentationContext());
        fShouldFilter = provider.areTreeModelViewerFiltersApplicable(element);
        fFilteredChildren = provider.getFilteredChildren(elementPath);
    }

    public synchronized void cancel() {
        if (fChildrenUpdate != null) {
            fChildrenUpdate.cancel();
        }
        super.cancel();
    }

    protected synchronized void scheduleViewerUpdate() {
        // If filtering is enabled perform child update on all children in order to update
        // viewer filters.
        if (fShouldFilter || fFilteredChildren != null) {
            if (fChildrenUpdate == null) {
                int startIdx;
                int count;
                if (fShouldFilter) {
                    startIdx = 0;
                    count = getCount();
                } else {
                    startIdx = fFilteredChildren[0];
                    int endIdx = fFilteredChildren[fFilteredChildren.length - 1];
                    count = endIdx - startIdx + 1;
                }

                fChildrenUpdate = new ChildrenUpdate(getContentProvider(), getViewerInput(), getElementPath(), getElement(), startIdx, count, getElementContentProvider()) {
                    protected void performUpdate() {
                        performUpdate(true);
                        ChildrenCountUpdate.super.scheduleViewerUpdate();
                    }

                    protected void scheduleViewerUpdate() {
                        execInDisplayThread(new Runnable() {
                            public void run() {
                                if (!getContentProvider().isDisposed() && !isCanceled()) {
                                    performUpdate();
                                }
                            }
                        });
                    }
                };
                execInDisplayThread(new Runnable() {
                    public void run() {
                        fChildrenUpdate.startRequest();
                    }
                });
                return;
            }
        } else {
            super.scheduleViewerUpdate();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.provisional.viewers.ViewerUpdateMonitor#performUpdate()
     */
    protected void performUpdate() {
        int viewCount = fCount;
        TreePath elementPath = getElementPath();
        if (viewCount == 0) {
            getContentProvider().clearFilters(elementPath);
        } else {
            getContentProvider().setModelChildCount(elementPath, fCount);
            viewCount = getContentProvider().modelToViewChildCount(elementPath, fCount);
        }
        if (DebugUIPlugin.DEBUG_CONTENT_PROVIDER && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(getPresentationContext())) {
            DebugUIPlugin.trace("setChildCount(" + getElement() + ", modelCount: " + fCount + " viewCount: " + viewCount + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        // Special case for element 0 in a set of filtered elements:
        // Child 0 is automatically updated by the tree at the same time that the child count is requested. Therefore,
        // If this child count update filtered out this element, it needs to be updated again.
        if (fShouldFilter && getContentProvider().isFiltered(elementPath, 0)) {
            getContentProvider().updateElement(elementPath, 0);
        }
        getContentProvider().getViewer().setChildCount(elementPath, viewCount);
        getContentProvider().getStateTracker().restorePendingStateOnUpdate(getElementPath(), -1, true, true, false);
    }

    public void setChildCount(int numChildren) {
        fCount = numChildren;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("IChildrenCountUpdate: "); //$NON-NLS-1$
        buf.append(getElement());
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.debug.internal.ui.viewers.model.ViewerUpdateMonitor#coalesce(org.eclipse.debug.internal.ui.viewers
     * .model.ViewerUpdateMonitor)
     */
    boolean coalesce(ViewerUpdateMonitor request) {
        if (request instanceof ChildrenCountUpdate) {
            if (getElementPath().equals(request.getElementPath())) {
                // duplicate request
                return true;
            } else if (getElementContentProvider().equals(request.getElementContentProvider())) {
                if (fBatchedRequests == null) {
                    fBatchedRequests = new ArrayList(4);
                    fBatchedRequests.add(this);
                }
                fBatchedRequests.add(request);
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.ViewerUpdateMonitor#startRequest()
     */
    void startRequest() {
        if (fBatchedRequests == null) {
            getElementContentProvider().update(new IChildrenCountUpdate[] { this });
        } else {
            IChildrenCountUpdate[] updates = (IChildrenCountUpdate[]) fBatchedRequests.toArray(new IChildrenCountUpdate[fBatchedRequests.size()]);
            // notify that the other updates have also started to ensure correct sequence
            // of model updates - **** start at index 1 since the first (0) update has
            // already notified the content provider that it has started.
            for (int i = 1; i < updates.length; i++) {
                getContentProvider().updateStarted((ViewerUpdateMonitor) updates[i]);
            }
            getElementContentProvider().update(updates);
        }
    }

    boolean containsUpdate(TreePath path) {
        if (getElementPath().equals(path)) {
            return true;
        } else if (fBatchedRequests != null) {
            for (int i = 0; i < fBatchedRequests.size(); i++) {
                if (((ViewerUpdateMonitor) fBatchedRequests.get(i)).getElementPath().equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.ViewerUpdateMonitor#getPriority()
     */
    int getPriority() {
        return 2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.ViewerUpdateMonitor#getSchedulingPath()
     */
    TreePath getSchedulingPath() {
        TreePath path = getElementPath();
        if (path.getSegmentCount() > 0) {
            return path.getParentPath();
        }
        return path;
    }

    int getCount() {
        return fCount;
    }

    protected boolean doEquals(ViewerUpdateMonitor update) {
        return update instanceof ChildrenCountUpdate &&
                getViewerInput().equals(update.getViewerInput()) &&
                getElementPath().equals(update.getElementPath());
    }

    protected int doHashCode() {
        return getClass().hashCode() + getViewerInput().hashCode() + getElementPath().hashCode();
    }
}
